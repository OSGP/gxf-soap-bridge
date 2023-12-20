// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.services;

import org.gxf.soapbridge.soap.clients.SoapClient;
import org.gxf.soapbridge.valueobjects.ProxyServerRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Service which can send SOAP requests to GXF. */
@Service
public class PlatformCommunicationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformCommunicationService.class);

  /** SOAP client used to sent request messages to GXF. */
  private final SoapClient soapClient;

  /** Service used to sign and/or verify the content of queue messages. */
  private final SigningService signingService;

  public PlatformCommunicationService(
      final SoapClient soapClient, final SigningService signingService) {
    this.soapClient = soapClient;
    this.signingService = signingService;
  }

  /**
   * Process an incoming queue message. The content of the message has to be verified by the {@link
   * SigningService}. Then a SOAP message can be sent to GXF using {@link SoapClient}.
   *
   * @param proxyServerRequestMessage The incoming queue message to process.
   */
  public void handleIncomingRequest(final ProxyServerRequestMessage proxyServerRequestMessage) {

    final String proxyServerRequestMessageAsString = proxyServerRequestMessage.constructString();
    final String securityKey = proxyServerRequestMessage.getSignature();

    final boolean isValid =
        signingService.verifyContent(proxyServerRequestMessageAsString, securityKey);
    if (!isValid) {
      LOGGER.error("ProxyServerRequestMessage failed to pass security check.");
      return;
    }

    final String connectionId = proxyServerRequestMessage.getConnectionId();
    final String context = proxyServerRequestMessage.getContext();
    final String commonName = proxyServerRequestMessage.getCommonName();
    final String soapPayload = proxyServerRequestMessage.getSoapPayload();

    soapClient.sendRequest(connectionId, context, commonName, soapPayload);
  }
}
