// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.endpoints;

import static org.springframework.security.web.context.RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.gxf.soapbridge.application.services.ConnectionCacheService;
import org.gxf.soapbridge.application.services.SigningService;
import org.gxf.soapbridge.configuration.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.kafka.senders.ProxyRequestKafkaSender;
import org.gxf.soapbridge.soap.clients.Connection;
import org.gxf.soapbridge.soap.exceptions.ConnectionNotFoundInCacheException;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.gxf.soapbridge.valueobjects.ProxyServerRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

/**
 * This {@link @Component} class is the endpoint for incoming SOAP requests from client
 * applications.
 */
@Component
public class SoapEndpoint implements HttpRequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SoapEndpoint.class);

  private static final String URL_PROXY_SERVER = "/proxy-server";

  private static final int INVALID_CUSTOM_TIME_OUT = -1;

  /** Service used to cache incoming connections from client applications. */
  private final ConnectionCacheService connectionCacheService;

  private final SoapConfigurationProperties soapConfiguration;

  /** Message sender which can send a webapp request message to ActiveMQ. */
  private final ProxyRequestKafkaSender proxyRequestsSender;

  /** Service used to sign the content of a message. */
  private final SigningService signingService;

  /** Map of time-outs for specific functions. */
  private final Map<String, Integer> customTimeOutsMap = new HashMap<>();

  public SoapEndpoint(
      final ConnectionCacheService connectionCacheService,
      final SoapConfigurationProperties soapConfiguration,
      final ProxyRequestKafkaSender proxyRequestsSender,
      final SigningService signingService) {
    this.connectionCacheService = connectionCacheService;
    this.soapConfiguration = soapConfiguration;
    this.proxyRequestsSender = proxyRequestsSender;
    this.signingService = signingService;
  }

  @PostConstruct
  public void init() {
    final String[] split = soapConfiguration.getCustomTimeouts().split(",");
    for (int i = 0; i < split.length; i += 2) {
      final String key = split[i];
      final Integer value = Integer.valueOf(split[i + 1]);
      LOGGER.debug("Adding custom time out with key: {} and value: {}", key, value);
      customTimeOutsMap.put(key, value);
    }
    LOGGER.debug("Added {} custom time outs to the map", customTimeOutsMap.size());
  }

  /** Handles incoming SOAP requests. */
  @Override
  public void handleRequest(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    // For debugging, print all headers and parameters.
    LOGGER.debug("Start of SoapEndpoint.handleRequest()");
    printHeaderValues(request);
    printParameterValues(request);

    // Get the context, which should be an OSGP SOAP end-point or a
    // NOTIFICATION SOAP end-point.
    final String context = getContextForRequestType(request);
    LOGGER.debug("Context: {}", context);

    // Try to read the SOAP request.
    final String soapPayload = readSoapPayload(request);
    if (soapPayload == null) {
      LOGGER.error("Unable to read SOAP request, returning 500.");
      createErrorResponse(response);
      return;
    }

    String organisationName = null;
    if (request.getAttribute(DEFAULT_REQUEST_ATTR_NAME)
            instanceof final SecurityContext securityContext
        && securityContext.getAuthentication().getPrincipal() instanceof final User organisation) {
      organisationName = organisation.getUsername();
    }
    if (organisationName == null) {
      LOGGER.error("Unable to find client certificate, returning 500.");
      createErrorResponse(response);
      return;
    }

    // Cache the incoming connection.
    final Connection newConnection = connectionCacheService.cacheConnection();
    final String connectionId = newConnection.getConnectionId();

    // Create a queue message and sign it.
    final ProxyServerRequestMessage requestMessage =
        new ProxyServerRequestMessage(connectionId, organisationName, context, soapPayload);
    try {
      final String signature = signingService.signContent(requestMessage.constructString());
      requestMessage.setSignature(signature);
    } catch (final ProxyServerException e) {
      LOGGER.error("Unable to sign message or set security key", e);
      createErrorResponse(response);
      connectionCacheService.removeConnection(connectionId);
      return;
    }

    final Integer customTimeOut = shouldUseCustomTimeOut(soapPayload);
    final int timeOut;
    if (customTimeOut == INVALID_CUSTOM_TIME_OUT) {
      timeOut = soapConfiguration.getTimeOut();
      LOGGER.debug("Using default time out: {} seconds", timeOut);
    } else {
      LOGGER.debug("Using custom time out: {} seconds", customTimeOut);
      timeOut = customTimeOut;
    }

    try {
      proxyRequestsSender.send(requestMessage);

      final boolean responseReceived = newConnection.waitForResponseReceived(timeOut);
      if (!responseReceived) {
        LOGGER.info("No response received within the specified time out of {} seconds", timeOut);
        createErrorResponse(response);
        connectionCacheService.removeConnection(connectionId);
        return;
      }
    } catch (final InterruptedException e) {
      LOGGER.error("Error while waiting for response", e);
      createErrorResponse(response);
      connectionCacheService.removeConnection(connectionId);
      Thread.currentThread().interrupt();
      return;
    }

    final String soap = readResponse(connectionId);
    if (soap == null) {
      LOGGER.error("Unable to read SOAP response: null");
      createErrorResponse(response);
    } else {
      LOGGER.debug("Request handled, trying to send response...");
      createSuccessFulResponse(response, soap);
    }

    LOGGER.debug(
        "End of SoapEndpoint.handleRequest() --> incoming request handled and response returned.");
  }

  private void printHeaderValues(final HttpServletRequest request) {
    if (LOGGER.isDebugEnabled()) {
      for (final Enumeration<String> headerNames = request.getHeaderNames();
          headerNames.hasMoreElements(); ) {
        final String headerName = headerNames.nextElement();
        final String headerValue = request.getHeader(headerName);
        LOGGER.debug(" header name: {} header value: {}", headerName, headerValue);
      }
    }
  }

  private void printParameterValues(final HttpServletRequest request) {
    if (LOGGER.isDebugEnabled()) {
      for (final Enumeration<String> parameterNames = request.getParameterNames();
          parameterNames.hasMoreElements(); ) {
        final String parameterName = parameterNames.nextElement();
        final String[] parameterValues = request.getParameterValues(parameterName);
        String str = "";
        for (final String parameterValue : parameterValues) {
          str = str.concat(parameterValue).concat(" ");
        }
        LOGGER.debug(" parameter name: {} parameter value: {}", parameterName, str);
      }
    }
  }

  private String getContextForRequestType(final HttpServletRequest request) {
    return request.getRequestURI().replace(URL_PROXY_SERVER, "");
  }

  private String readSoapPayload(final HttpServletRequest request) {
    final StringBuilder stringBuilder = new StringBuilder();
    String line;
    String soapPayload = null;
    try {
      final BufferedReader reader = request.getReader();
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
      }
      soapPayload = stringBuilder.toString();
      LOGGER.debug(" payload: {}", soapPayload);
    } catch (final Exception e) {
      LOGGER.error("Unexpected error while reading request body", e);
    }
    return soapPayload;
  }

  private Integer shouldUseCustomTimeOut(final String soapPayload) {
    final Set<String> keys = customTimeOutsMap.keySet();
    for (final String key : keys) {
      if (soapPayload.contains(key)) {
        return customTimeOutsMap.get(key);
      }
    }
    return INVALID_CUSTOM_TIME_OUT;
  }

  private String readResponse(final String connectionId) throws ServletException {
    final String soap;
    try {
      final Connection connection = connectionCacheService.findConnection(connectionId);
      soap = connection.getSoapResponse();
      connectionCacheService.removeConnection(connectionId);
    } catch (final ConnectionNotFoundInCacheException e) {
      LOGGER.error("Unexpected error while trying to find a cached connection", e);
      throw new ServletException("Unable to obtain response");
    }
    return soap;
  }

  private void createErrorResponse(final HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  private void createSuccessFulResponse(final HttpServletResponse response, final String soap)
      throws IOException {
    LOGGER.debug("Start - creating successful response");
    response.setStatus(HttpServletResponse.SC_OK);
    response.addHeader("SOAP-ACTION", "");
    response.addHeader("Keep-Alive", "timeout=5, max=100");
    response.addHeader("Accept", "text/xml");
    response.addHeader("Connection", "Keep-Alive");
    response.setContentType("text/xml; charset=" + StandardCharsets.UTF_8.name());
    response.getWriter().write(soap);
    LOGGER.debug("End - creating successful response");
  }
}
