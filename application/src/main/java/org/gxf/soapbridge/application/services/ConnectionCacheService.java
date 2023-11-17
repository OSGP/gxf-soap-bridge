// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.services;

import java.util.concurrent.ConcurrentHashMap;
import org.gxf.soapbridge.soap.clients.Connection;
import org.gxf.soapbridge.soap.exceptions.ConnectionNotFoundInCacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** This {@link @Service} class caches connections from client applications. */
@Service
public class ConnectionCacheService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionCacheService.class);

  /**
   * Map used to cache connections. The key is an uuid. The value is a {@link Connection} instance.
   */
  private static final ConcurrentHashMap<String, Connection> cache = new ConcurrentHashMap<>();

  /**
   * Creates a connection and puts it in the cache.
   *
   * @return the created Connection
   */
  public Connection cacheConnection() {
    final Connection connection = new Connection();
    final String connectionId = connection.getConnectionId();
    LOGGER.debug("Caching connection with connectionId: {}", connectionId);
    cache.put(connectionId, connection);
    return connection;
  }

  /**
   * Get a {@link Connection} instance from the {@link ConnectionCacheService#cache}.
   *
   * @param connectionId The key for the {@link Connection} instance obtained by calling {@link
   *     ConnectionCacheService#cacheConnection()}.
   * @return A {@link Connection} instance.
   * @throws ConnectionNotFoundInCacheException In case the connection is not present in the {@link
   *     ConnectionCacheService#cache}.
   */
  public Connection findConnection(final String connectionId)
      throws ConnectionNotFoundInCacheException {
    LOGGER.debug("Trying to find connection with connectionId: {}", connectionId);
    final Connection connection = cache.get(connectionId);
    if (connection == null) {
      throw new ConnectionNotFoundInCacheException(
          String.format("Unable to find connection for connectionId: %s", connectionId));
    }
    return connection;
  }

  /**
   * Removes a {@link Connection} instance from the {@link ConnectionCacheService#cache}.
   *
   * @param connectionId The key for the {@link Connection} instance obtained by calling {@link
   *     ConnectionCacheService#cacheConnection()}.
   */
  public void removeConnection(final String connectionId) {
    LOGGER.debug("Removing connection with connectionId: {}", connectionId);
    cache.remove(connectionId);
  }
}
