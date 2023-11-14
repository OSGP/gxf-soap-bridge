// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.endpoints;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Rdn;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import org.gxf.soapbridge.application.services.ConnectionCacheService;
import org.gxf.soapbridge.application.services.SigningService;
import org.gxf.soapbridge.configuration.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.kafka.senders.ProxyRequestKafkaSender;
import org.gxf.soapbridge.soap.clients.Connection;
import org.gxf.soapbridge.soap.exceptions.ConnectionNotFoundInCacheException;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.gxf.soapbridge.soap.valueobjects.ClientCertificate;
import org.gxf.soapbridge.valueobjects.ProxyServerRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This {@link @Component} class is the endpoint for incoming SOAP requests from client
 * applications.
 */
@Component
public class SoapEndpoint implements HttpRequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SoapEndpoint.class);

  private static final String SOAP_HEADER_KEY_APPLICATION_NAME = "ApplicationName";
  private static final String SOAP_HEADER_KEY_ORGANISATION_IDENTIFICATION =
      "OrganisationIdentification";
  private static final String SOAP_HEADER_KEY_USER_NAME = "UserName";
  private static final List<String> SOAP_HEADER_KEYS =
      List.of(
          SOAP_HEADER_KEY_APPLICATION_NAME,
          SOAP_HEADER_KEY_ORGANISATION_IDENTIFICATION,
          SOAP_HEADER_KEY_USER_NAME);

  private static final String URL_PROXY_SERVER = "/proxy-server";

  private static final int INVALID_CUSTOM_TIME_OUT = -1;
  public static final String X_509_CERTIFICATE_REQUEST_ATTRIBUTE =
      "jakarta.servlet.request.X509Certificate";

  /** Service used to cache incoming connections from client applications. */
  @Autowired private ConnectionCacheService connectionCacheService;

  @Autowired private SoapConfigurationProperties soapConfiguration;

  /** Message sender which can send a webapp request message to ActiveMQ. */
  @Autowired private ProxyRequestKafkaSender proxyRequestsSender;

  /** Service used to sign the content of a message. */
  @Autowired private SigningService signingService;

  /** Map of time outs for specific functions. */
  private final Map<String, Integer> customTimeOutsMap = new HashMap<>();

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
    if (request.getAttribute(RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME)
        instanceof final SecurityContext securityContext) {
      if (securityContext.getAuthentication().getPrincipal() instanceof final User organisation) {
        organisationName = organisation.getUsername();
      }
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
    } catch (final Exception e) {
      LOGGER.info("Error while waiting for response", e);
      createErrorResponse(response);
      connectionCacheService.removeConnection(connectionId);
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

  public String[] sander() {
    return new String[] {"a", "poipoi", "frats"};
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

  private Map<String, String> getSoapHeaderValues(
      final String soapPayload, final List<String> soapHeaderKeys) {
    final Map<String, String> values = new HashMap<>();
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setNamespaceAware(false);
      final InputStream inputStream =
          new ByteArrayInputStream(soapPayload.getBytes(StandardCharsets.UTF_8));
      // Try to find the desired XML elements in the document.
      final Document document = factory.newDocumentBuilder().parse(inputStream);
      for (final String soapHeaderKey : soapHeaderKeys) {
        final String value = evaluateXPathExpression(document, soapHeaderKey);
        values.put(soapHeaderKey, value);
      }
      inputStream.close();
    } catch (final Exception e) {
      LOGGER.error("Exception", e);
    }
    return values;
  }

  /**
   * Search an XML element using an XPath expression.
   *
   * @param document The XML document.
   * @param element The name of the desired XML element.
   * @return The content of the XML element, or null if the element is not found.
   * @throws XPathExpressionException In case the expression fails to compile or evaluate, an
   *     exception will be thrown.
   */
  private String evaluateXPathExpression(final Document document, final String element)
      throws XPathExpressionException {
    final String expression = String.format("//*[contains(local-name(), '%s')]", element);

    final XPathFactory xFactory = XPathFactory.newInstance();
    final XPath xPath = xFactory.newXPath();

    final XPathExpression xPathExpression = xPath.compile(expression);
    final NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);

    if (nodeList != null && nodeList.getLength() > 0) {
      return nodeList.item(0).getTextContent();
    } else {
      return null;
    }
  }

  private ClientCertificate tryToFindClientCertificate(
      final HttpServletRequest request, final String soapPayload) {
    final X509Certificate[] x509Certificates = getX509CertificatesFromServlet(request);
    ClientCertificate clientCertificate = null;

    if (x509Certificates.length == 0) {
      LOGGER.error(" HTTPServletRequest's attribute was an empty array of X509Certificates.");
    } else if (x509Certificates.length == 1) {
      LOGGER.debug(" HTTPServletRequest's attribute was array of X509Certificates of length 1.");

      // Get the client certificate.
      clientCertificate = getClientCertificate(x509Certificates[0]);
    } else {
      LOGGER.debug(
          " HTTPServletRequest's attribute was array of X509Certificates of length {}.",
          x509Certificates.length);

      final Map<String, String> soapHeaderValues =
          getSoapHeaderValues(soapPayload, SOAP_HEADER_KEYS);
      LOGGER.debug(
          " SOAP Header ApplicationName: {}",
          soapHeaderValues.get(SOAP_HEADER_KEY_APPLICATION_NAME));
      LOGGER.debug(
          " SOAP Header OrganisationIdentification: {}",
          soapHeaderValues.get(SOAP_HEADER_KEY_ORGANISATION_IDENTIFICATION));
      LOGGER.debug(" SOAP Header UserName: {}", soapHeaderValues.get(SOAP_HEADER_KEY_USER_NAME));

      // Try to extract the client certificate for the organization
      // identification.
      clientCertificate =
          getClientCertificateByOrganisationIdentification(
              x509Certificates, soapHeaderValues.get(SOAP_HEADER_KEY_ORGANISATION_IDENTIFICATION));
    }

    return clientCertificate;
  }

  private X509Certificate[] getX509CertificatesFromServlet(final HttpServletRequest request) {
    final Object x509CertificateAttribute =
        request.getAttribute(X_509_CERTIFICATE_REQUEST_ATTRIBUTE);
    LOGGER.debug(X_509_CERTIFICATE_REQUEST_ATTRIBUTE + ": {}", x509CertificateAttribute);
    if (x509CertificateAttribute instanceof X509Certificate[]) {
      LOGGER.debug(" x509CertificateAttribute instanceof X509Certificate[]");
      return (X509Certificate[]) x509CertificateAttribute;
    } else {
      return new X509Certificate[0];
    }
  }

  private ClientCertificate getClientCertificateByOrganisationIdentification(
      final X509Certificate[] array, final String organisationCommonName) {

    for (final X509Certificate x509Certificate : array) {
      final Principal principal = x509Certificate.getSubjectDN();
      LOGGER.debug(" principal: {}", principal);
      final String subjectDn = principal.getName();
      LOGGER.debug(" subjectDn: {}", subjectDn);
      try {
        final String commonName = getCommonName(subjectDn);
        if (commonName.equals(organisationCommonName)) {
          LOGGER.debug(
              "Found client certificate for right organisation {}", organisationCommonName);
          return new ClientCertificate(x509Certificate, commonName);
        }
      } catch (final NamingException e) {
        LOGGER.info("Failed to extract CommonName from this ClientCertificate", e);
      }
    }
    return null;
  }

  private ClientCertificate getClientCertificate(final X509Certificate x509Certificate) {
    ClientCertificate clientCertificate = null;

    final Principal principal = x509Certificate.getSubjectDN();
    LOGGER.debug(" principal: {}", principal);
    final String subjectDn = principal.getName();
    LOGGER.debug(" subjectDn: {}", subjectDn);
    try {
      final String commonName = getCommonName(subjectDn);
      clientCertificate = new ClientCertificate(x509Certificate, commonName);
    } catch (final NamingException e) {
      LOGGER.info("Failed to extract CommonName from ClientCertificate", e);
    }

    return clientCertificate;
  }

  private String getCommonName(final String subjectDn) throws NamingException {
    final Rdn rdn = new Rdn(subjectDn);
    LOGGER.debug(" rdn: {}", rdn);
    final Attributes attributes = rdn.toAttributes();
    LOGGER.debug(" attributes: {}", attributes);
    final Attribute attribute = attributes.get("cn");
    LOGGER.debug(" attribute: {}", attribute);
    final String commonName = (String) attribute.get();
    LOGGER.debug(" common name: {}", commonName);
    return commonName;
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
