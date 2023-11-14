// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.application.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.gxf.soapbridge.soap.endpoints.SoapEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

@Component
public class SoapConfiguration extends AbstractHandlerMapping {
  private final SoapEndpoint soapEndpoint;

  public SoapConfiguration(final SoapEndpoint soapEndpoint) {
    this.soapEndpoint = soapEndpoint;
  }

  @Override
  protected Object getHandlerInternal(final HttpServletRequest request) {
    return soapEndpoint;
  }
}
