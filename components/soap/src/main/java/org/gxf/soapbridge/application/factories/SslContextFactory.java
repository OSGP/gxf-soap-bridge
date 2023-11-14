// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.application.factories;

import org.gxf.soapbridge.application.properties.SecurityConfigurationProperties;
import org.gxf.soapbridge.application.properties.StoreConfigurationProperties;
import org.gxf.soapbridge.application.services.SslContextCacheService;
import org.gxf.soapbridge.soap.exceptions.UnableToCreateKeyManagersException;
import org.gxf.soapbridge.soap.exceptions.UnableToCreateTrustManagersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * This {@link @Component} class can create {@link SSLContext} instances.
 */
@Component
public class SslContextFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslContextFactory.class);

    /**
     * In order to create a proper instance of {@link SSLContext}, a protocol must be specified. Note
     * that if {@link SSLContext#getDefault()} is used, the created instance does not support the
     * custom {@link TrustManager} and {@link KeyManager} which are needed for this application!!!
     */
    private static final String SSL_CONTEXT_PROTOCOL = "TLS";

    @Autowired
    private SecurityConfigurationProperties securityConfiguration;

    /**
     * Using the trust store, a {@link TrustManager} instance is created. This instance is created
     * once, and assigned to this field. Subsequent calls to
     * {@link SslContextCacheService#getSslContext()} will use the instance in order to create
     * {@link SSLContext} instances.
     */
    private TrustManager[] trustManagersForHttps;

    /**
     * Using the trust store, a {@link TrustManager} instance is created. This instance is created
     * once, and assigned to this field. Subsequent calls to
     * {@link SslContextCacheService#getSslContextForCommonName(String)} will use the instance in
     * order to create {@link SSLContext} instances.
     */
    private TrustManager[] trustManagersForHttpsWithClientCertificate;

    /**
     * Create an {@link SSLContext} instance.
     *
     * @return An {@link SSLContext} instance.
     */
    public SSLContext createSslContext() {
        return createSslContext("");
    }

    /**
     * Create an {@link SSLContext} instance for the given common name.
     *
     * @param commonName The common name used to open the key store for an organization. May be an
     *                   empty string if no client certificate is required.
     * @return An {@link SSLContext} instance.
     */
    public SSLContext createSslContext(final String commonName) {
        if (commonName.isEmpty()) {
            try {
                // Only create an instance of the trust manager array once.
                if (trustManagersForHttps == null) {
                    trustManagersForHttps = openTrustStoreAndCreateTrustManagers();
                }
                // Use the trust manager to initialize an SSLContext instance.
                final SSLContext sslContext = SSLContext.getInstance(SSL_CONTEXT_PROTOCOL);
                sslContext.init(null, trustManagersForHttps, null);
                LOGGER.info("Created SSL context using trust manager for HTTPS");
                return sslContext;
            } catch (final Exception e) {
                LOGGER.error("Unexpected exception while creating SSL context using trust manager", e);
                return null;
            }
        } else {
            try {
                // Only create an instance of the trust manager array once.
                if (trustManagersForHttpsWithClientCertificate == null) {
                    trustManagersForHttpsWithClientCertificate =
                            openTrustStoreAndCreateTrustManagers();
                }
                final KeyManager[] keyManagerArray = openKeyStoreAndCreateKeyManagers(commonName);
                // Use the key manager and trust manager to initialize an
                // SSLContext instance.
                final SSLContext sslContext = SSLContext.getInstance(SSL_CONTEXT_PROTOCOL);
                sslContext.init(keyManagerArray, trustManagersForHttpsWithClientCertificate, null);
                // It is not possible to set the SSLContext instance as default
                // using: "SSLContext.setDefault(sslContext);" The SSl context
                // is unique for each organization because each organization has
                // their own *.pfx key store, which is the client certificate.
                LOGGER.info("Created SSL context using trust manager and key manager for HTTPS");
                return sslContext;
            } catch (final Exception e) {
                LOGGER.error(
                        "Unexpected exception while creating SSL context using trust manager and key manager",
                        e);
                return null;
            }
        }
    }

    private TrustManager[] openTrustStoreAndCreateTrustManagers()
            throws UnableToCreateTrustManagersException {
        final StoreConfigurationProperties trustStore = securityConfiguration.getTrustStore();
        LOGGER.debug("Opening trust store, pathToTrustStore: {}", trustStore.getLocation());
        try (final InputStream trustStream = new FileInputStream(trustStore.getLocation())) {
            // Create trust manager using *.jks file.
            final char[] trustPassword = trustStore.getPassword().toCharArray();
            final KeyStore trustStoreInstance = KeyStore.getInstance(trustStore.getType());
            trustStoreInstance.load(trustStream, trustPassword);
            final TrustManagerFactory trustFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory.init(trustStoreInstance);
            return trustFactory.getTrustManagers();
        } catch (final Exception e) {
            throw new UnableToCreateTrustManagersException(
                    "Unexpected exception while creating trust managers using trust store", e);
        }
    }

    private KeyManager[] openKeyStoreAndCreateKeyManagers(final String commonName)
            throws UnableToCreateKeyManagersException {
        // Assume the path does not have a trailing slash ( '/' ) and assume the
        // file extension to be *.pfx.
        final StoreConfigurationProperties keyStore = securityConfiguration.getKeyStore();
        final String pathToKeyStore = String.format("%s/%s.pfx", keyStore.getLocation(), commonName);
        LOGGER.debug("Opening key store, pathToKeyStore: {}", pathToKeyStore);
        try (final InputStream keyStoreStream = new FileInputStream(pathToKeyStore)) {
            // Create key manager using *.pfx file.
            final KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final KeyStore keyStoreInstance = KeyStore.getInstance(keyStore.getType());
            final char[] keyPassword = keyStore.getPassword().toCharArray();
            keyStoreInstance.load(keyStoreStream, keyPassword);
            keyManagerFactory.init(keyStoreInstance, keyPassword);
            return keyManagerFactory.getKeyManagers();
        } catch (final Exception e) {
            throw new UnableToCreateKeyManagersException(
                    "Unexpected exception while creating key managers using key store", e);
        }
    }
}
