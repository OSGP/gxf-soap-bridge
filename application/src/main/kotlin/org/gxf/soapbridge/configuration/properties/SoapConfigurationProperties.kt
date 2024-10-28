// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("soap")
class SoapConfigurationProperties(
    val hostnameVerificationStrategy: HostnameVerificationStrategy,
    /**
     * Maximum number of seconds this {@link SoapEndpoint} will wait for a response from the other end before
     * terminating the connection with the client application.
     */
    val timeout: Int,
    /** Timeouts for specific functions. */
    val customTimeouts: Map<String, Int> = emptyMap(),
    /**
     * TODO Can we search for certificates on both sides
     *
     * Property to set common name based on the organisation on requests published to Kafka.
     *
     * If set to false the other listening proxy doesn't search for certificates by
     * [org.gxf.soapbridge.valueobjects.ProxyServerRequestMessage.commonName]. Instead, the other proxy will generate a
     * new ssl context.
     */
    val useOrganisationFromRequest: Boolean = true,
    val callEndpoint: SoapEndpointConfiguration,
)

enum class HostnameVerificationStrategy {
    ALLOW_ALL_HOSTNAMES,
    BROWSER_COMPATIBLE_HOSTNAMES
}

class SoapEndpointConfiguration(host: String, port: Int, protocol: String) {
    // TODO Use java.net.URI class
    val hostAndPort = "$host:$port"
    val uri = "$protocol://${hostAndPort}"
}
