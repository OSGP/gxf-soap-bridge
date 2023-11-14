// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.soap.exceptions;

public class UnableToCreateTrustManagersException extends ProxyServerException {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -855694158211466200L;

    public UnableToCreateTrustManagersException(final String message, final Throwable t) {
        super(message, t);
    }
}
