// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.application.services;

import org.gxf.soapbridge.messaging.messages.ProxyServerResponseMessage;
import org.gxf.soapbridge.services.ProxyResponseHandler;
import org.gxf.soapbridge.soap.clients.Connection;
import org.gxf.soapbridge.soap.exceptions.ConnectionNotFoundInCacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service which handles SOAP responses from OSGP. The SOAP response will be set for the connection
 * which correlates with the connection-id.
 */
@Service
public class ClientCommunicationService implements ProxyResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommunicationService.class);

    /**
     * Service used to cache incoming connections from client applications.
     */
    @Autowired
    private ConnectionCacheService connectionCacheService;

    /**
     * Service used to sign and/or verify the content of queue messages.
     */
    @Autowired
    private SigningService signingService;

    /**
     * Process an incoming queue message. The content of the message has to be verified by the
     * {@link SigningService}. Then a response from OSGP will set for the pending connection from a
     * client.
     *
     * @param proxyServerResponseMessage The incoming queue message to process.
     */
    @Override
    public void handleIncomingResponse(final ProxyServerResponseMessage proxyServerResponseMessage) {
        final boolean isValid = signingService.verifyContent(
                proxyServerResponseMessage.constructString(), proxyServerResponseMessage.getSignature());
        if (!isValid) {
            LOGGER.error("ProxyServerResponseMessage failed to pass security check.");
            return;
        }

        try {
            final Connection connection =
                    connectionCacheService.findConnection(proxyServerResponseMessage.getConnectionId());
            if (connection != null) {
                if (isValid) {
                    LOGGER.debug("Connection valid, set SOAP response");
                    connection.setResponse(proxyServerResponseMessage.getSoapResponse());
                } else {
                    connection.setResponse("Security check has failed.");
                }
            } else {
                LOGGER.error("Cached connection is null");
            }
        } catch (final ConnectionNotFoundInCacheException e) {
            LOGGER.error("ConnectionNotFoundInCacheException", e);
        }
    }
}
