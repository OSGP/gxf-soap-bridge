// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.services;

import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.gxf.soapbridge.application.factories.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This {@link @Service} class encapsulates the creation of a suitable {@link SSLContext} instance
 * to use with a HTTPS connection, like {@link HttpsURLConnection} for example. Further, the created
 * instance is cached using a {@link ConcurrentHashMap} in order to maintain performance even when
 * many calls are handled simultaneously. The actual creation of {@link SSLContext} instances is
 * delegated to {@link SslContextFactory} class.
 */
@Service
public class SslContextCacheService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SslContextCacheService.class);

  /**
   * Map used to cache {@link SSLContext} instances. The key is the common name for an organization,
   * which is equal to the organization identification.
   */
  private static final ConcurrentHashMap<String, SSLContext> cache = new ConcurrentHashMap<>();

  /** Factory which assists in creating {@link SSLContext} instances. */
  private final SslContextFactory sslContextFactory;

  public SslContextCacheService(final SslContextFactory sslContextFactory) {
    this.sslContextFactory = sslContextFactory;
  }

  /**
   * Creates a new {@link SSLContext} instance and caches it, or fetches an existing instance from
   * the cache.
   *
   * @return A {@link SSLContext} instance.
   */
  public SSLContext getSslContext() {
    final String key = "SSLContextWithoutCommonName";
    if (cache.containsKey(key)) {
      LOGGER.debug("Returning SSL Context from cache for key: {}", key);
      return cache.get(key);
    } else {
      LOGGER.debug(
          "Creating new SSL Context and putting the instance in the cache for key: {}", key);
      final SSLContext sslContext = sslContextFactory.createSslContext();
      cache.put(key, sslContext);
      return sslContext;
    }
  }

  /**
   * Creates a new {@link SSLContext} instance for the given common name and caches it, or fetches
   * an existing instance from the cache.
   *
   * @param commonName The common name which is used to look up a Personal Information Exchange
   *     (*.pfx) file that is used as key store.
   * @return A {@link SSLContext} instance.
   */
  public SSLContext getSslContextForCommonName(final String commonName) {
    if (cache.containsKey(commonName)) {
      LOGGER.debug("Returning SSL Context from cache for common name: {}", commonName);
      return cache.get(commonName);
    } else {
      LOGGER.debug(
          "Creating new SSL Context and putting the instance in the cache for common name: {}",
          commonName);
      final SSLContext sslContext = sslContextFactory.createSslContext(commonName);
      cache.put(commonName, sslContext);
      return sslContext;
    }
  }
}
