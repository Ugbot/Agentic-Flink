package com.ververica.flink.agent.storage;

import com.ververica.flink.agent.storage.memory.InMemoryLongTermStore;
import com.ververica.flink.agent.storage.memory.InMemoryShortTermStore;
import com.ververica.flink.agent.storage.postgres.PostgresConversationStore;
import com.ververica.flink.agent.storage.redis.RedisConversationStore;
import com.ververica.flink.agent.storage.redis.RedisShortTermStore;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating storage provider instances.
 *
 * <p>This factory provides a centralized way to create and configure storage backends for
 * different tiers (HOT, WARM). It supports multiple backend implementations per tier and handles
 * initialization.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Create storage providers
 * Map<String, String> redisConfig = new HashMap<>();
 * redisConfig.put("redis.host", "localhost");
 * redisConfig.put("redis.port", "6379");
 *
 * ShortTermMemoryStore hotStore = StorageFactory.createShortTermStore("redis", redisConfig);
 * LongTermMemoryStore warmStore = StorageFactory.createLongTermStore("redis", redisConfig);
 *
 * // Use in processing
 * hotStore.putItems("flow-001", contextItems);
 * warmStore.saveContext("flow-001", agentContext);
 * }</pre>
 *
 * <p>Supported backends:
 *
 * <ul>
 *   <li>Short-term (HOT): "memory", "redis"
 *   <li>Long-term (WARM): "memory", "redis", "postgresql"
 * </ul>
 *
 * @author Agentic Flink Team
 */
public class StorageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(StorageFactory.class);

  /**
   * Create a short-term memory store (HOT tier).
   *
   * <p>Supported backends:
   *
   * <ul>
   *   <li>"memory" - In-memory ConcurrentHashMap with TTL
   *   <li>"redis" - Redis with Jedis client
   * </ul>
   *
   * @param backend Backend identifier
   * @param config Backend-specific configuration
   * @return Initialized ShortTermMemoryStore instance
   * @throws Exception if creation or initialization fails
   */
  public static ShortTermMemoryStore createShortTermStore(
      String backend, Map<String, String> config) throws Exception {

    if (backend == null || backend.trim().isEmpty()) {
      throw new IllegalArgumentException("Backend cannot be null or empty");
    }

    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    LOG.info("Creating short-term store: backend={}", backend);

    ShortTermMemoryStore store;

    switch (backend.toLowerCase()) {
      case "memory":
        store = new InMemoryShortTermStore();
        break;

      case "redis":
        store = new RedisShortTermStore();
        break;

      default:
        throw new IllegalArgumentException(
            "Unknown short-term store backend: " + backend + ". Supported: memory, redis");
    }

    store.initialize(config);
    LOG.info("Short-term store initialized: backend={}, tier={}", backend, store.getTier());
    return store;
  }

  /**
   * Create a long-term memory store (WARM tier).
   *
   * <p>Supported backends:
   *
   * <ul>
   *   <li>"memory" - In-memory conversation storage (for testing/development)
   *   <li>"redis" - Redis with conversation persistence
   *   <li>"postgresql" - PostgreSQL with ACID guarantees
   * </ul>
   *
   * @param backend Backend identifier
   * @param config Backend-specific configuration
   * @return Initialized LongTermMemoryStore instance
   * @throws Exception if creation or initialization fails
   */
  public static LongTermMemoryStore createLongTermStore(
      String backend, Map<String, String> config) throws Exception {

    if (backend == null || backend.trim().isEmpty()) {
      throw new IllegalArgumentException("Backend cannot be null or empty");
    }

    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    LOG.info("Creating long-term store: backend={}", backend);

    LongTermMemoryStore store;

    switch (backend.toLowerCase()) {
      case "memory":
        store = new InMemoryLongTermStore();
        break;

      case "redis":
        store = new RedisConversationStore();
        break;

      case "postgresql":
      case "postgres":
        store = new PostgresConversationStore();
        break;

      default:
        throw new IllegalArgumentException(
            "Unknown long-term store backend: "
                + backend
                + ". Supported: memory, redis, postgresql");
    }

    store.initialize(config);
    LOG.info("Long-term store initialized: backend={}, tier={}", backend, store.getTier());
    return store;
  }

  /**
   * Create a steering state store (WARM tier).
   *
   * <p>Supported backends:
   *
   * <ul>
   *   <li>"redis" - Redis for steering configuration
   *   <li>"dynamodb" - AWS DynamoDB (not yet implemented)
   *   <li>"memory" - In-memory for testing (not yet implemented)
   * </ul>
   *
   * @param backend Backend identifier
   * @param config Backend-specific configuration
   * @return Initialized SteeringStateStore instance
   * @throws Exception if creation or initialization fails
   */
  public static SteeringStateStore createSteeringStore(
      String backend, Map<String, String> config) throws Exception {

    LOG.info("Creating steering store: backend={}", backend);

    throw new UnsupportedOperationException(
        "SteeringStateStore implementations not yet available. "
            + "Planned backends: redis, dynamodb, memory");
  }

  /**
   * Create a vector store (VECTOR tier).
   *
   * <p>Supported backends:
   *
   * <ul>
   *   <li>"qdrant" - Qdrant vector database (not yet implemented)
   *   <li>"pinecone" - Pinecone (not yet implemented)
   *   <li>"weaviate" - Weaviate (not yet implemented)
   *   <li>"pgvector" - PostgreSQL with pgvector extension (not yet implemented)
   * </ul>
   *
   * @param backend Backend identifier
   * @param config Backend-specific configuration
   * @return Initialized VectorStore instance
   * @throws Exception if creation or initialization fails
   */
  public static VectorStore createVectorStore(String backend, Map<String, String> config)
      throws Exception {

    LOG.info("Creating vector store: backend={}", backend);

    throw new UnsupportedOperationException(
        "VectorStore implementations not yet available. "
            + "Planned backends: qdrant, pinecone, weaviate, pgvector");
  }

  /**
   * Create a storage provider from configuration.
   *
   * <p>This method reads the tier and backend from configuration and creates the appropriate
   * storage provider.
   *
   * <p>Configuration keys:
   *
   * <ul>
   *   <li>storage.tier - One of: HOT, WARM, COLD, VECTOR
   *   <li>storage.backend - Backend identifier for the tier
   *   <li>Additional backend-specific keys
   * </ul>
   *
   * @param config Configuration map
   * @return Initialized StorageProvider instance
   * @throws Exception if creation or initialization fails
   */
  public static StorageProvider<?, ?> createFromConfig(Map<String, String> config)
      throws Exception {

    String tierStr = config.get("storage.tier");
    String backend = config.get("storage.backend");

    if (tierStr == null || backend == null) {
      throw new IllegalArgumentException(
          "Configuration must include 'storage.tier' and 'storage.backend'");
    }

    StorageTier tier = StorageTier.valueOf(tierStr.toUpperCase());

    LOG.info("Creating storage provider from config: tier={}, backend={}", tier, backend);

    switch (tier) {
      case HOT:
        return createShortTermStore(backend, config);

      case WARM:
        return createLongTermStore(backend, config);

      case COLD:
        throw new UnsupportedOperationException(
            "Cold tier storage not yet implemented. Use HOT or WARM tier.");

      case VECTOR:
        return createVectorStore(backend, config);

      case CHECKPOINT:
        throw new UnsupportedOperationException(
            "CHECKPOINT tier is managed by Flink, not by this factory");

      default:
        throw new IllegalArgumentException("Unknown storage tier: " + tier);
    }
  }

  /**
   * Get available backends for a storage tier.
   *
   * @param tier Storage tier
   * @return Array of backend identifiers
   */
  public static String[] getAvailableBackends(StorageTier tier) {
    switch (tier) {
      case HOT:
        return new String[] {"memory", "redis"};

      case WARM:
        return new String[] {"memory", "redis", "postgresql"};

      case CHECKPOINT:
        return new String[] {"rocksdb", "hashmap"}; // Managed by Flink

      case COLD:
      case VECTOR:
      default:
        return new String[] {};
    }
  }

  /**
   * Check if a backend is available for a tier.
   *
   * @param tier Storage tier
   * @param backend Backend identifier
   * @return true if backend is available for this tier
   */
  public static boolean isBackendAvailable(StorageTier tier, String backend) {
    String[] available = getAvailableBackends(tier);
    for (String b : available) {
      if (b.equalsIgnoreCase(backend)) {
        return true;
      }
    }
    return false;
  }
}
