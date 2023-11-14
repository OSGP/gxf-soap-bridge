// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.soap.valueobjects;

import java.security.cert.X509Certificate;

public class ClientCertificate {

    private final X509Certificate x509Certificate;

    private final String commonName;

    public ClientCertificate(final X509Certificate x509Certificate, final String commonName) {
        this.x509Certificate = x509Certificate;
        this.commonName = commonName;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public String getCommonName() {
        return commonName;
    }
}
