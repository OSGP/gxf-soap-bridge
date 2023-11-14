// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.soap.exceptions;

public class UnableToCreateHttpsURLConnectionException extends ProxyServerException {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -8807766325167125880L;

    public UnableToCreateHttpsURLConnectionException(final String message) {
        super(message);
    }
}
