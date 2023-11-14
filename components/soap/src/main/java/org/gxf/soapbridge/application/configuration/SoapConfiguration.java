// Copyright 2023 Alliander N.V.

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
