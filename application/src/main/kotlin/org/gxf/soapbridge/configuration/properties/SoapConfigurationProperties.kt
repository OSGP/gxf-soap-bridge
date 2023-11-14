// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("soap")
class SoapConfigurationProperties(
    val hostnameVerificationStrategy: HostnameVerificationStrategy,
    /**
     * Maximum number of seconds this {@link SoapEndpoint} will wait for a response from the other end
     * before terminating the connection with the client application.
     */
    val timeOut: Int,
    /**
     * Time outs for specific functions.
     */
    val customTimeouts: String,
    val callEndpoint: SoapEndpointConfiguration,
)

enum class HostnameVerificationStrategy {
    ALLOW_ALL_HOSTNAMES, BROWSER_COMPATIBLE_HOSTNAMES
}

class SoapEndpointConfiguration(
    host: String,
    port: Int,
    protocol: String
) {

    val hostAndPort = "$host:$port"
    val uri = "$protocol://${hostAndPort}"
}