package com.ververica.flink.agent.storage;

import java.io.Serializable;

/**
 * Factory for creating agent storage backends.
 *
 * <p>Provides a unified interface for configuring and creating storage implementations:
 * <ul>
 *   <li>PostgreSQL (WARM tier, durable storage)</li>
 *   <li>Redis/Valkey (HOT tier, fast caching)</li>
 *   <li>In-Memory (for testing)</li>
 * </ul>
 *
 * <p><b>Placeholder Implementation:</b>
 * This is a stub for Phase 2.4 (Job Generator). The full storage backend integration
 * will be implemented in Phase 4 (Storage & State Management) with:
 * <ul>
 *   <li>Tiered storage (Hot/Warm/Cold)</li>
 *   <li>Async I/O for non-blocking persistence</li>
 *   <li>State offloading for large contexts</li>
 *   <li>Checkpoint integration</li>
 * </ul>
 *
 * @author Agentic Flink Team
 */
public class AgentStorageFactory {

  /**
   * Configuration for storage backends.
   */
  public static class StorageConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String postgresUrl;
    private String postgresUser;
    private String postgresPassword;
    private String redisHost;
    private int redisPort;
    private boolean enableTiering;

    public String getPostgresUrl() { return postgresUrl; }
    public void setPostgresUrl(String postgresUrl) { this.postgresUrl = postgresUrl; }

    public String getPostgresUser() { return postgresUser; }
    public void setPostgresUser(String postgresUser) { this.postgresUser = postgresUser; }

    public String getPostgresPassword() { return postgresPassword; }
    public void setPostgresPassword(String postgresPassword) { this.postgresPassword = postgresPassword; }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

    public boolean isEnableTiering() { return enableTiering; }
    public void setEnableTiering(boolean enableTiering) { this.enableTiering = enableTiering; }

    /**
     * Creates default storage config (PostgreSQL + Redis).
     */
    public static StorageConfig defaults() {
      StorageConfig config = new StorageConfig();
      config.setPostgresUrl("jdbc:postgresql://localhost:5432/agentic_flink");
      config.setPostgresUser("flink");
      config.setPostgresPassword("flink");
      config.setRedisHost("localhost");
      config.setRedisPort(6379);
      config.setEnableTiering(true);
      return config;
    }

    /**
     * Creates in-memory storage config (for testing).
     */
    public static StorageConfig inMemory() {
      StorageConfig config = new StorageConfig();
      config.setEnableTiering(false);
      return config;
    }
  }
}
