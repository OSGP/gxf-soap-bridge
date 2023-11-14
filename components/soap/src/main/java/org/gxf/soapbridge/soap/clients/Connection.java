// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.soap.clients;

import org.gxf.soapbridge.application.services.ConnectionCacheService;
import org.gxf.soapbridge.application.utils.RandomStringFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Connection {

    /**
     * Default length for the connection id, which is the key for the
     * {@link ConnectionCacheService#cache}.
     */
    private static final int CONNECTION_ID_LENGTH = 32;

    private volatile boolean responseResolved;

    private String soapResponse;

    private String connectionId;

    private final Semaphore responseReceived;

    public Connection() {
        responseResolved = false;
        responseReceived = new Semaphore(0);
        connectionId = RandomStringFactory.generateRandomString(CONNECTION_ID_LENGTH);
    }

    public boolean isResponseResolved() {
        return responseResolved;
    }

    public void setResponse(final String soapResponse) {
        responseResolved = true;
        this.soapResponse = soapResponse;
        responseReceived();
    }

    public String getSoapResponse() {
        return soapResponse;
    }

    public void setConnectionId(final String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    /*
     * Indicates the response for this connection has been received.
     */
    public void responseReceived() {
        responseReceived.release();
    }

    /*
     * Waits for a response on this connection.
     *
     * @timeout The number of seconds to wait for a response.
     *
     * @returns true, if the response was received within @timeout seconds,
     * false otherwise.
     */
    public boolean waitForResponseReceived(final int timeout) throws InterruptedException {
        return responseReceived.tryAcquire(timeout, TimeUnit.SECONDS);
    }
}
