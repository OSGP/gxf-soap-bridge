// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.clients;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.gxf.soapbridge.application.factories.HttpsUrlConnectionFactory;
import org.gxf.soapbridge.application.services.SigningService;
import org.gxf.soapbridge.configuration.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.configuration.properties.SoapEndpointConfiguration;
import org.gxf.soapbridge.kafka.senders.ProxyResponseKafkaSender;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.gxf.soapbridge.soap.exceptions.UnableToCreateHttpsURLConnectionException;
import org.gxf.soapbridge.valueobjects.ProxyServerResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** This {@link @Component} class can send SOAP messages to the Platform. */
@Component
public class SoapClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SoapClient.class);

  /** Message sender to send messages to a queue. */
  private final ProxyResponseKafkaSender proxyReponseSender;

  private final SoapConfigurationProperties soapConfiguration;

  /** Factory which assist in creating {@link HttpsURLConnection} instances. */
  private final HttpsUrlConnectionFactory httpsUrlConnectionFactory;

  /** Service used to sign the content of a message. */
  private final SigningService signingService;

  public SoapClient(
      final ProxyResponseKafkaSender proxyReponseSender,
      final SoapConfigurationProperties soapConfiguration,
      final HttpsUrlConnectionFactory httpsUrlConnectionFactory,
      final SigningService signingService) {
    this.proxyReponseSender = proxyReponseSender;
    this.soapConfiguration = soapConfiguration;
    this.httpsUrlConnectionFactory = httpsUrlConnectionFactory;
    this.signingService = signingService;
  }

  /**
   * Send a request to the Platform.
   *
   * @param connectionId The connectionId for this connection.
   * @param context The part of the URL indicating the SOAP web-service.
   * @param commonName The common name (organisation identification).
   * @param soapPayload The SOAP message to send to the platform.
   * @return True if the request has been sent and the response has been received and written to a
   *     queue, false otherwise.
   */
  public boolean sendRequest(
      final String connectionId,
      final String context,
      final String commonName,
      final String soapPayload) {

    HttpsURLConnection connection = null;

    try {
      // Try to create a connection.
      connection = createConnection(context, soapPayload, commonName);
      if (connection == null) {
        LOGGER.warn("Could not create connection for sending SOAP request.");
        return false;
      }

      // Send the SOAP payload to the server.
      sendRequest(connection, soapPayload);
      // Read the response.
      LOGGER.debug("SOAP request sent, trying to read SOAP response...");
      final String soapResponse = readResponse(connection);
      LOGGER.debug("SOAP response: {}", soapResponse);
      // Always disconnect the connection.
      connection.disconnect();

      // Create proxy-server response message.
      final ProxyServerResponseMessage responseMessage =
          createProxyServerResponseMessage(connectionId, soapResponse);

      // Send queue message.
      proxyReponseSender.send(responseMessage);

      return true;
    } catch (final Exception e) {
      if (connection != null) {
        connection.disconnect();
      }
      LOGGER.error("Unexpected exception while sending SOAP request", e);
      return false;
    }
  }

  private HttpsURLConnection createConnection(
      final String context, final String soapPayload, final String commonName)
      throws UnableToCreateHttpsURLConnectionException {
    final String contentLength = String.format("%d", soapPayload.length());

    final SoapEndpointConfiguration callEndpoint = soapConfiguration.getCallEndpoint();

    final String uri = callEndpoint.getUri().concat(context);
    LOGGER.info("Preparing to open connection for WEBAPP_REQUEST using URI: {}", uri);
    return httpsUrlConnectionFactory.createConnection(
        uri, callEndpoint.getHostAndPort(), contentLength, commonName);
  }

  private void sendRequest(final HttpsURLConnection connection, final String soapPayLoad)
      throws IOException {
    try (final OutputStream outputStream = connection.getOutputStream();
        final OutputStreamWriter outputStreamWriter =
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      outputStreamWriter.write(soapPayLoad);
      outputStreamWriter.flush();
    } catch (final IOException e) {
      LOGGER.debug("Rethrow IOException while sending SOAP request.");
      throw e;
    }
  }

  private String readResponse(final HttpsURLConnection connection) throws IOException {
    // Use a BufferedReader and an InputStreamReader configured with UTF-8
    // character encoding. This will ensure that the response from the
    // Platform is read correctly.
    try (final InputStream inputStream = getInputStream(connection);
        final InputStreamReader inputStreamReader =
            new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final BufferedReader reader = new BufferedReader(inputStreamReader)) {
      final StringBuilder response = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        response.append(line);
        line = reader.readLine();
      }
      return response.toString();
    } catch (final IOException e) {
      LOGGER.debug("Rethrow IOException while reading SOAP response");
      throw e;
    }
  }

  private InputStream getInputStream(final HttpsURLConnection connection) throws IOException {
    final InputStream inputStream;

    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
      inputStream = connection.getInputStream();
    } else {
      inputStream = connection.getErrorStream();
    }

    return inputStream;
  }

  private ProxyServerResponseMessage createProxyServerResponseMessage(
      final String connectionId, final String soapResponse) throws ProxyServerException {
    final ProxyServerResponseMessage responseMessage =
        new ProxyServerResponseMessage(connectionId, soapResponse);
    final String signature = signingService.signContent(responseMessage.constructString());
    responseMessage.setSignature(signature);
    return responseMessage;
  }
}
