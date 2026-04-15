package com.ververica.flink.agent.storage.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ververica.flink.agent.config.ConfigKeys;
import com.ververica.flink.agent.context.core.ContextItem;
import com.ververica.flink.agent.storage.ShortTermMemoryStore;
import com.ververica.flink.agent.storage.StorageTier;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis-based implementation of ShortTermMemoryStore using Jedis.
 *
 * <p>This implementation stores short-term memory in Redis for fast access with persistence and
 * optional clustering support. Suitable for distributed Flink deployments where multiple task
 * managers need shared access to context.
 *
 * <p>Characteristics:
 *
 * <ul>
 *   <li>Latency: &lt;1ms for local Redis, 1-5ms for remote
 *   <li>Capacity: Limited by Redis memory
 *   <li>Persistence: Optional (RDB/AOF)
 *   <li>Distribution: Shared across all Flink task managers
 * </ul>
 *
 * <p>Data Structure:
 *
 * <pre>
 * agent:shortterm:{flowId} -> JSON list of ContextItems
 * </pre>
 *
 * <p>Configuration:
 *
 * <pre>{@code
 * Map<String, String> config = new HashMap<>();
 * config.put("redis.host", "localhost");
 * config.put("redis.port", "6379");
 * config.put("redis.password", "secret");  // Optional
 * config.put("redis.database", "0");  // Default: 0
 * config.put("redis.ttl.seconds", "3600");  // Default: 1 hour
 * config.put("redis.pool.max.total", "50");  // Connection pool size
 * config.put("redis.pool.max.idle", "10");
 * config.put("redis.timeout.ms", "2000");
 * }</pre>
 *
 * <p>Dependency required (add to pom.xml):
 *
 * <pre>{@code
 * <dependency>
 *     <groupId>redis.clients</groupId>
 *     <artifactId>jedis</artifactId>
 *     <version>5.1.0</version>
 * </dependency>
 * <dependency>
 *     <groupId>com.fasterxml.jackson.core</groupId>
 *     <artifactId>jackson-databind</artifactId>
 *     <version>2.15.2</version>
 * </dependency>
 * }</pre>
 *
 * <p>Status: Production-ready implementation with full Redis integration. Uncomment Jedis imports
 * and code after adding dependencies.
 *
 * @author Agentic Flink Team
 */
public class RedisShortTermStore implements ShortTermMemoryStore {

  private static final Logger LOG = LoggerFactory.getLogger(RedisShortTermStore.class);
  private static final String KEY_PREFIX = "agent:shortterm:";

  // Redis connection pool
  private transient JedisPool jedisPool;

  // Configuration
  private String host;
  private int port;
  private String password;
  private int database;
  private long defaultTTLSeconds;
  private int timeout;

  // JSON serialization
  private transient ObjectMapper objectMapper;

  // Statistics
  private transient long hitCount = 0;
  private transient long missCount = 0;
  private transient long putCount = 0;

  @Override
  public void initialize(Map<String, String> config) throws Exception {
    // Parse configuration
    this.host = config.getOrDefault(ConfigKeys.REDIS_HOST, ConfigKeys.DEFAULT_REDIS_HOST);
    this.port = Integer.parseInt(config.getOrDefault(ConfigKeys.REDIS_PORT, ConfigKeys.DEFAULT_REDIS_PORT));
    this.password = config.get(ConfigKeys.REDIS_PASSWORD); // Optional
    this.database = Integer.parseInt(config.getOrDefault("redis.database", "0"));
    this.defaultTTLSeconds =
        Long.parseLong(config.getOrDefault("redis.ttl.seconds", "3600"));
    this.timeout = Integer.parseInt(config.getOrDefault("redis.timeout.ms", "2000"));

    // Connection pool configuration
    int maxTotal = Integer.parseInt(config.getOrDefault("redis.pool.max.total", "50"));
    int maxIdle = Integer.parseInt(config.getOrDefault("redis.pool.max.idle", "10"));

    // Initialize JSON mapper
    this.objectMapper = new ObjectMapper();

    // Initialize Jedis pool
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(maxTotal);
    poolConfig.setMaxIdle(maxIdle);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);

    if (password != null && !password.isEmpty()) {
      this.jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
    } else {
      this.jedisPool = new JedisPool(poolConfig, host, port, timeout, null, database);
    }

    LOG.info(
        "RedisShortTermStore initialized: host={}, port={}, database={}, ttl={}s",
        host, port, database, defaultTTLSeconds);
  }

  @Override
  public void put(String key, List<ContextItem> value) throws Exception {
    putItems(key, value);
  }

  @Override
  public Optional<List<ContextItem>> get(String key) throws Exception {
    List<ContextItem> items = getItems(key);
    return items.isEmpty() ? Optional.empty() : Optional.of(items);
  }

  @Override
  public void putItems(String flowId, List<ContextItem> items) throws Exception {
    if (flowId == null || items == null) {
      throw new IllegalArgumentException("flowId and items cannot be null");
    }

    String key = KEY_PREFIX + flowId;
    String json = objectMapper.writeValueAsString(items);

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.set(key, json);
      jedis.expire(key, defaultTTLSeconds);
      putCount++;
      LOG.debug("Stored {} items for flow {} in Redis", items.size(), flowId);
    }
  }

  @Override
  public List<ContextItem> getItems(String flowId) throws Exception {
    if (flowId == null) {
      throw new IllegalArgumentException("flowId cannot be null");
    }

    String key = KEY_PREFIX + flowId;

    try (Jedis jedis = jedisPool.getResource()) {
      String json = jedis.get(key);
      if (json != null) {
        hitCount++;
        List<ContextItem> items = objectMapper.readValue(json,
            new TypeReference<List<ContextItem>>() {});
        LOG.debug("Retrieved {} items for flow {} from Redis", items.size(), flowId);
        return items;
      } else {
        missCount++;
        return new ArrayList<>();
      }
    }
  }

  @Override
  public void addItem(String flowId, ContextItem item) throws Exception {
    if (flowId == null || item == null) {
      throw new IllegalArgumentException("flowId and item cannot be null");
    }

    // Retrieve existing items, add new item, store back
    List<ContextItem> items = getItems(flowId);
    items.add(item);
    putItems(flowId, items);

    LOG.debug("Added item to flow {} in Redis: {}", flowId, item.getItemId());
  }

  @Override
  public void removeItem(String flowId, String itemId) throws Exception {
    if (flowId == null || itemId == null) {
      throw new IllegalArgumentException("flowId and itemId cannot be null");
    }

    List<ContextItem> items = getItems(flowId);
    items.removeIf(item -> itemId.equals(item.getItemId()));
    putItems(flowId, items);

    LOG.debug("Removed item {} from flow {} in Redis", itemId, flowId);
  }

  @Override
  public int getItemCount(String flowId) throws Exception {
    if (flowId == null) {
      throw new IllegalArgumentException("flowId cannot be null");
    }

    try (Jedis jedis = jedisPool.getResource()) {
      String key = KEY_PREFIX + flowId;
      String json = jedis.get(key);
      if (json != null) {
        List<ContextItem> items = objectMapper.readValue(json,
            new TypeReference<List<ContextItem>>() {});
        return items.size();
      }
      return 0;
    }
  }

  @Override
  public void clearItems(String flowId) throws Exception {
    if (flowId == null) {
      throw new IllegalArgumentException("flowId cannot be null");
    }

    String key = KEY_PREFIX + flowId;

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(key);
      LOG.debug("Cleared items for flow {} in Redis", flowId);
    }
  }

  @Override
  public void delete(String key) throws Exception {
    clearItems(key);
  }

  @Override
  public boolean exists(String key) throws Exception {
    if (key == null) {
      return false;
    }

    String redisKey = KEY_PREFIX + key;

    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.exists(redisKey);
    }
  }

  @Override
  public Map<String, Object> getStatistics() throws Exception {
    Map<String, Object> stats = new HashMap<>();

    stats.put("hit_count", hitCount);
    stats.put("miss_count", missCount);
    stats.put("put_count", putCount);
    stats.put("ttl_seconds", defaultTTLSeconds);
    stats.put("redis_host", host);
    stats.put("redis_port", port);
    stats.put("redis_database", database);

    try (Jedis jedis = jedisPool.getResource()) {
      // Get Redis server info
      String info = jedis.info("memory");
      // Parse memory usage, keys count, etc.
      stats.put("redis_connected", true);
      stats.put("redis_info", info);
    } catch (Exception e) {
      stats.put("redis_connected", false);
      stats.put("redis_error", e.getMessage());
    }

    return stats;
  }

  @Override
  public void setTTL(String flowId, long ttlSeconds) throws Exception {
    if (flowId == null) {
      throw new IllegalArgumentException("flowId cannot be null");
    }

    String key = KEY_PREFIX + flowId;

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.expire(key, ttlSeconds);
      LOG.debug("Set TTL for flow {} to {} seconds in Redis", flowId, ttlSeconds);
    }
  }

  @Override
  public void close() throws Exception {
    if (jedisPool != null) {
      jedisPool.close();
      LOG.info("RedisShortTermStore connection pool closed");
    }
  }

  @Override
  public StorageTier getTier() {
    return StorageTier.HOT;
  }

  @Override
  public long getExpectedLatencyMs() {
    return 1; // Sub-millisecond for local Redis
  }
}
