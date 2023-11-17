// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.factories;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.gxf.soapbridge.application.services.SslContextCacheService;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.gxf.soapbridge.soap.exceptions.UnableToCreateHttpsURLConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** This {@link @Component} class can create {@link HttpsURLConnection} instances. */
@Component
public class HttpsUrlConnectionFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpsUrlConnectionFactory.class);

  /** Cache of {@link SSLContext} instances used to obtain {@link HttpsURLConnection} instances. */
  private final SslContextCacheService sslContextCacheService;

  private final HostnameVerifierFactory hostnameVerifierFactory;

  public HttpsUrlConnectionFactory(
      final SslContextCacheService sslContextCacheService,
      final HostnameVerifierFactory hostnameVerifierFactory) {
    this.sslContextCacheService = sslContextCacheService;
    this.hostnameVerifierFactory = hostnameVerifierFactory;
  }

  /**
   * Create an {@link HttpsURLConnection} instance for the given arguments.
   *
   * @param uri The full URI of the end-point for this connection.
   * @param host The host consists of domain name, server name or IP address followed by the port.
   *     Example: localhost:443
   * @param contentLength The content length of the SOAP payload which will be sent to the end-point
   *     using this connection.
   * @param commonName The common name for the organization.
   * @return Null in case the {@link SSLContext} cannot be created or fetched from the {@link
   *     SslContextCacheService}, or a configured and initialized {@link HttpsURLConnection}
   *     instance.
   * @throws UnableToCreateHttpsURLConnectionException In case the configuration and/or
   *     initialization of an {@link HttpsURLConnection} instance fails.
   */
  public HttpsURLConnection createConnection(
      final String uri, final String host, final String contentLength, final String commonName)
      throws UnableToCreateHttpsURLConnectionException {
    try {
      // Get SSLContext instance.
      final SSLContext sslContext;
      if (StringUtils.hasText(commonName)) {
        sslContext = sslContextCacheService.getSslContextForCommonName(commonName);
      } else {
        sslContext = sslContextCacheService.getSslContext();
      }
      // Check SSLContext instance.
      if (sslContext == null) {
        LOGGER.error(
            "SSLContext instance is null. Unable to create HttpsURLConnection instance for uri: {}, host: {}, content length: {}, common name: {}",
            uri,
            host,
            contentLength,
            commonName);
        return null;
      }
      // Create connection.
      final HttpsURLConnection connection = (HttpsURLConnection) new URL(uri).openConnection();
      connection.setHostnameVerifier(hostnameVerifierFactory.getHostnameVerifier());
      connection.setSSLSocketFactory(sslContext.getSocketFactory());
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty(
          "Accept-Encoding", "text/xml;charset=" + StandardCharsets.UTF_8.name());
      connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
      connection.setRequestProperty(
          "Content-Type", "text/xml;charset=" + StandardCharsets.UTF_8.name());
      connection.setRequestProperty("SOAP-ACTION", "");
      connection.setRequestProperty("Content-Length", contentLength);
      connection.setRequestProperty("Host", host);
      connection.setRequestProperty("Connection", "Keep-Alive");
      connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.155 Safari/537.36");
      LOGGER.debug(
          "Created HttpsURLConnection instance for uri: {}, host: {}, content length: {}, common name: {}",
          uri,
          host,
          contentLength,
          commonName);

      return connection;
    } catch (final IOException | ProxyServerException e) {
      throw new UnableToCreateHttpsURLConnectionException("Creating connection failed.", e);
    }
  }
}
