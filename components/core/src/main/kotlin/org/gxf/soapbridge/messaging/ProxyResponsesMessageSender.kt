// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.messaging

import org.gxf.soapbridge.messaging.messages.ProxyServerResponseMessage

interface ProxyResponsesMessageSender {
    fun send(responseMessage: ProxyServerResponseMessage)
}
