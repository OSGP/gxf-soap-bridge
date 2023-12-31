// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.services;

import org.gxf.soapbridge.soap.clients.Connection;
import org.gxf.soapbridge.valueobjects.ProxyServerResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service which handles SOAP responses from GXF. The SOAP response will be set for the connection
 * which correlates with the connection-id.
 */
@Service
public class ClientCommunicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommunicationService.class);

    /**
     * Service used to cache incoming connections from client applications.
     */
    private final ConnectionCacheService connectionCacheService;

    /**
     * Service used to sign and/or verify the content of queue messages.
     */
    private final SigningService signingService;

    public ClientCommunicationService(
            final ConnectionCacheService connectionCacheService, final SigningService signingService) {
        this.connectionCacheService = connectionCacheService;
        this.signingService = signingService;
    }

    /**
     * Process an incoming queue message. The content of the message has to be verified by the {@link
     * SigningService}. Then a response from GXF will set for the pending connection from a client.
     *
     * @param proxyServerResponseMessage The incoming queue message to process.
     */
    public void handleIncomingResponse(final ProxyServerResponseMessage proxyServerResponseMessage) {
        final boolean isValid =
                signingService.verifyContent(
                        proxyServerResponseMessage.constructString(),
                        proxyServerResponseMessage.getSignature());

        final Connection connection =
                connectionCacheService.findConnection(proxyServerResponseMessage.getConnectionId());

        if (connection == null) {
            LOGGER.error("No connection found in cache for id: {}", proxyServerResponseMessage.getConnectionId());
            return;
        }

        if (isValid) {
            LOGGER.debug("Connection valid, set SOAP response");
            connection.setSoapResponse(proxyServerResponseMessage.getSoapResponse());
        } else {
            LOGGER.error("ProxyServerResponseMessage failed to pass security check.");
            connection.setSoapResponse("Security check has failed.");
        }
    }
}
