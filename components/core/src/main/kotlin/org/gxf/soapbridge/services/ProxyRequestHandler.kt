// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.services

import org.gxf.soapbridge.messaging.messages.ProxyServerRequestMessage

interface ProxyRequestHandler {
    fun handleIncomingRequest(proxyServerRequestMessage: ProxyServerRequestMessage)
}
