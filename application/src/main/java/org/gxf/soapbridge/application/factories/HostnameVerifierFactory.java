// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.application.factories;

import static org.gxf.soapbridge.configuration.properties.HostnameVerificationStrategy.ALLOW_ALL_HOSTNAMES;
import static org.gxf.soapbridge.configuration.properties.HostnameVerificationStrategy.BROWSER_COMPATIBLE_HOSTNAMES;

import javax.net.ssl.HostnameVerifier;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.gxf.soapbridge.configuration.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.springframework.stereotype.Component;

@Component
public class HostnameVerifierFactory {
  private final SoapConfigurationProperties soapConfiguration;

  public HostnameVerifierFactory(final SoapConfigurationProperties soapConfiguration) {
    this.soapConfiguration = soapConfiguration;
  }

  public HostnameVerifier getHostnameVerifier() throws ProxyServerException {
    if (this.soapConfiguration.getHostnameVerificationStrategy() == ALLOW_ALL_HOSTNAMES) {
      return new NoopHostnameVerifier();
    } else if (this.soapConfiguration.getHostnameVerificationStrategy()
        == BROWSER_COMPATIBLE_HOSTNAMES) {
      return new DefaultHostnameVerifier();
    } else {
      throw new ProxyServerException("No hostname verification strategy set!");
    }
  }
}
