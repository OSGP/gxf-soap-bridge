// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.valueobjects

import mu.KotlinLogging
import org.gxf.soapbridge.exceptions.ProxyMessageException
import java.util.*

class ProxyServerResponseMessage(connectionId: String, val soapResponse: String) :
    ProxyServerBaseMessage(connectionId) {


    /** Constructs a string separated by '~' from the fields of this instance.  */
    override fun getFieldsForMessage(): List<String> = listOf(
        connectionId,
        encode(soapResponse)
    )

    companion object {
        val logger = KotlinLogging.logger { }

        /**
         * Constructs a ProxyServerResponseMessage instance from a string separated by '~'.
         *
         * @param string The input string.
         * @return A ProxyServerResponseMessage instance.
         * @throws ProxyMessageException
         */
        @Throws(ProxyMessageException::class)
        fun createInstanceFromString(string: String): ProxyServerResponseMessage {
            val split = string.split(SEPARATOR)
            val numTokens = split.size
            logger.debug { "split.length: ${numTokens}" }
            if (numTokens < 3) {
                throw ProxyMessageException(
                    "Invalid number of tokens, don't try to create ProxyServerResponseMessage"
                )
            }
            if (logger.isDebugEnabled) {
                logger.debug("split[0] connection-id: {}", split[0])
                logger.debug("split[1] encoded soap-response length: {}", split[1].length)
                logger.debug("split[1] soap-response: {}", decode(split[1]))
                logger.debug("split[2] security-key : {}", split[2])
            }
            val proxyServerResponseMessage = ProxyServerResponseMessage(split[0], decode(split[1]))
            proxyServerResponseMessage.signature = split[2]
            return proxyServerResponseMessage
        }
    }
}
