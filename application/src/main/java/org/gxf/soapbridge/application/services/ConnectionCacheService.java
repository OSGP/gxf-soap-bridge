// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.services;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.gxf.soapbridge.monitoring.MonitoringService;
import org.gxf.soapbridge.soap.clients.Connection;
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

  private final MonitoringService monitoringService;

  public ConnectionCacheService(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @PostConstruct
  public void postConstructor() {
    monitoringService.monitorCacheSize(cache);
  }

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
   * @return A {@link Connection} instance. If no connection with the id is present return null.
   */
  @Nullable
  public Connection findConnection(final String connectionId) {
    LOGGER.debug("Trying to find connection with connectionId: {}", connectionId);
    return cache.get(connectionId);
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
