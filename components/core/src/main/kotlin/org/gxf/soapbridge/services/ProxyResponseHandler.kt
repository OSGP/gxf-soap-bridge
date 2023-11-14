// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.services

import org.gxf.soapbridge.messaging.messages.ProxyServerResponseMessage

interface ProxyResponseHandler {
    fun handleIncomingResponse(proxyServerResponseMessage: ProxyServerResponseMessage)
}
