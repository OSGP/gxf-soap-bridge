// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.application.services;

import org.gxf.soapbridge.messaging.messages.ProxyServerRequestMessage;
import org.gxf.soapbridge.services.ProxyRequestHandler;
import org.gxf.soapbridge.soap.clients.SoapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service which can send SOAP requests to OSGP.
 */
@Service
public class PlatformCommunicationService implements ProxyRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformCommunicationService.class);

    /**
     * SOAP client used to sent request messages to OSGP.
     */
    @Autowired
    private SoapClient soapClient;

    /**
     * Service used to sign and/or verify the content of queue messages.
     */
    @Autowired
    private SigningService signingService;

    /**
     * Process an incoming queue message. The content of the message has to be verified by the
     * {@link SigningService}. Then a SOAP message can be sent to OSGP using {@link SoapClient}.
     *
     * @param proxyServerRequestMessage The incoming queue message to process.
     */
    @Override
    public void handleIncomingRequest(
            final ProxyServerRequestMessage proxyServerRequestMessage) {

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

        final boolean result =
                soapClient.sendRequest(
                        connectionId, context, commonName, soapPayload);
        if (!result) {
            LOGGER.error("Unsuccessful at sending request to platform.");
        } else {
            LOGGER.debug("Successfully sent response message to queue");
        }
    }
}
