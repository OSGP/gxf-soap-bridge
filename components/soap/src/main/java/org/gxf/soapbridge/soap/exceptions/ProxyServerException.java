// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.soap.exceptions;

/**
 * Base type for exceptions for proxy server component.
 */
public class ProxyServerException extends Exception {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -8696835428244659385L;

    public ProxyServerException() {
        super();
    }

    public ProxyServerException(final String message) {
        super(message);
    }

    public ProxyServerException(final String message, final Throwable t) {
        super(message, t);
    }
}
