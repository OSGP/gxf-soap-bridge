// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.valueobjects

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gxf.soapbridge.exceptions.ProxyMessageException

class ProxyServerRequestMessage(
    connectionId: String,
    val commonName: String,
    val context: String,
    val soapPayload: String,
) : ProxyServerBaseMessage(connectionId) {

    override fun getFieldsForMessage(): List<String> =
        listOf(connectionId, encode(context), encode(soapPayload), encode(commonName))

    companion object {
        private val LOGGER = KotlinLogging.logger {}

        /**
         * Constructs a ProxyServerRequestMessage instance from a string separated by '~'.
         *
         * @param string The input string.
         * @return A ProxyServerRequestMessage instance.
         * @throws ProxyMessageException
         */
        @Throws(ProxyMessageException::class)
        fun createInstanceFromString(string: String): ProxyServerRequestMessage {
            val split = string.split(SEPARATOR)
            val numTokens = split.size
            LOGGER.debug { "split.length: $numTokens" }
            if (numTokens < 4 || numTokens > 5) {
                throw ProxyMessageException("Invalid number of tokens, not trying to create ProxyServerRequestMessage.")
            }
            if (LOGGER.isDebugEnabled()) {
                printValues(numTokens, split)
            }
            val connectionId = split[0]
            val context = decode(split[1])
            val soapRequest = decode(split[2])
            val commonName: String
            val signature: String
            if (numTokens == 4) {
                // No common name used.
                commonName = ""
                signature = split[3]
            } else {
                // Common name used.
                commonName = decode(split[3])
                signature = split[4]
            }
            val proxyServerRequestMessage = ProxyServerRequestMessage(connectionId, commonName, context, soapRequest)
            proxyServerRequestMessage.signature = signature
            return proxyServerRequestMessage
        }

        private fun printValues(numTokens: Int, split: List<String?>) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug { "split[0] connection-id: ${split[0]}" }
                LOGGER.debug { "split[2] context      : ${decode(split[1])}" }
                LOGGER.debug { "split[3] encoded soap-request length: ${split[2]!!.length}" }
                LOGGER.debug { "split[3] soap-request : ${decode(split[2])}" }
                if (numTokens == 5) {
                    LOGGER.debug { "split[4] security-key : ${split[3]}" }
                } else {
                    LOGGER.debug { "split[4] common-name  : ${decode(split[3])}" }
                    LOGGER.debug { "split[5] security-signature : ${split[4]}" }
                }
            }
        }
    }
}
