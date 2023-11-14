// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.messaging

import org.gxf.soapbridge.messaging.messages.ProxyServerRequestMessage

interface ProxyRequestsMessageSender {
    fun send(requestMessage: ProxyServerRequestMessage)
}
