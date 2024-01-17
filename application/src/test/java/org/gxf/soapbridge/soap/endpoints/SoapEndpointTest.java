// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.soap.endpoints;

import org.gxf.soapbridge.configuration.properties.HostnameVerificationStrategy;
import org.gxf.soapbridge.configuration.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.configuration.properties.SoapEndpointConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.HashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SoapEndpointTest {

    @Test
    @ParameterizedTest
    void getContextForRequestType() {
        var soapConfigurationProperties = new SoapConfigurationProperties(
                HostnameVerificationStrategy.ALLOW_ALL_HOSTNAMES,
                0,
                new HashMap<>(),
                true,
                new SoapEndpointConfiguration("test", 443, "https")
        );
        var soapEndpoint = new SoapEndpoint(
                null,
                soapConfigurationProperties,
                null,
                null,
                null
        );


        assertThat(soapEndpoint.getContextForRequestURI("/proxy-server/notifications/web-api-net-management/notificationService"))
                .isEqualTo("/web-api-net-management/notificationService");

        assertThat(soapEndpoint.getContextForRequestURI("/proxy-server/web-api-net-management/notificationService"))
                .isEqualTo("/web-api-net-management/notificationService");

        assertThat(soapEndpoint.getContextForRequestURI("/web-api-net-management/notificationService"))
                .isEqualTo("/web-api-net-management/notificationService");

    }
}
