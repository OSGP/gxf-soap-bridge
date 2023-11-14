// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.application.factories;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.gxf.soapbridge.application.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;

import static org.gxf.soapbridge.application.properties.HostnameVerificationStrategy.ALLOW_ALL_HOSTNAMES;
import static org.gxf.soapbridge.application.properties.HostnameVerificationStrategy.BROWSER_COMPATIBLE_HOSTNAMES;

@Component
public class HostnameVerifierFactory {
    private final SoapConfigurationProperties soapConfiguration;

    public HostnameVerifierFactory(final SoapConfigurationProperties soapConfiguration) {
        this.soapConfiguration = soapConfiguration;
    }

    public HostnameVerifier getHostnameVerifier() throws ProxyServerException {
        if (soapConfiguration.getHostnameVerificationStrategy() == ALLOW_ALL_HOSTNAMES) {
            return new NoopHostnameVerifier();
        } else if (soapConfiguration.getHostnameVerificationStrategy() == BROWSER_COMPATIBLE_HOSTNAMES) {
            return new DefaultHostnameVerifier();
        } else {
            throw new ProxyServerException("No hostname verification strategy set!");
        }
    }
}
