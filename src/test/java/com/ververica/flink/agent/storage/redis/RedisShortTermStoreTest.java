package com.ververica.flink.agent.storage.redis;

import static org.junit.jupiter.api.Assertions.*;

import com.ververica.flink.agent.context.core.ContextItem;
import com.ververica.flink.agent.context.core.ContextPriority;
import com.ververica.flink.agent.context.core.MemoryType;
import com.ververica.flink.agent.storage.StorageTier;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for RedisShortTermStore.
 *
 * <p>NOTE: These tests require a running Redis instance.
 * Run with Docker: docker run -d -p 6379:6379 redis:7-alpine --requirepass test_password
 *
 * <p>Tests are marked with @Disabled by default to avoid breaking builds without Redis.
 * Enable them when you have Redis running locally or in CI/CD.
 *
 * @author Agentic Flink Team
 */
@Disabled("Requires running Redis instance - enable for integration testing")
class RedisShortTermStoreTest {

  private RedisShortTermStore store;
  private Map<String, String> config;
  private String testFlowId;

  @BeforeEach
  void setUp() throws Exception {
    config = new HashMap<>();
    config.put("redis.host", "localhost");
    config.put("redis.port", "6379");
    config.put("redis.password", "test_password");
    config.put("redis.database", "0");
    config.put("redis.ttl.seconds", "300");

    store = new RedisShortTermStore();
    store.initialize(config);

    testFlowId = "test-flow-" + UUID.randomUUID();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (store != null) {
      // Clean up test data
      try {
        store.delete(testFlowId);
      } catch (Exception e) {
        // Ignore cleanup errors
      }
      store.close();
    }
  }

  @Test
  @DisplayName("Should initialize with correct tier and latency")
  void testInitialization() {
    assertEquals(StorageTier.HOT, store.getTier());
    assertTrue(store.getExpectedLatencyMs() < 5);
    assertEquals("RedisShortTermStore", store.getProviderName());
  }

  @Test
  @DisplayName("Should store and retrieve context items")
  void testPutAndGetItems() throws Exception {
    List<ContextItem> items = Arrays.asList(
        createTestItem("Message 1", ContextPriority.MUST),
        createTestItem("Message 2", ContextPriority.SHOULD)
    );

    store.putItems(testFlowId, items);

    List<ContextItem> retrieved = store.getItems(testFlowId);
    assertFalse(retrieved.isEmpty());
    assertEquals(2, retrieved.size());
  }

  @Test
  @DisplayName("Should return empty list for non-existent flow")
  void testGetNonExistent() throws Exception {
    List<ContextItem> retrieved = store.getItems("does-not-exist");
    assertTrue(retrieved.isEmpty());
  }

  @Test
  @DisplayName("Should delete items")
  void testDelete() throws Exception {
    List<ContextItem> items = Arrays.asList(
        createTestItem("Message 1", ContextPriority.MUST)
    );
    store.putItems(testFlowId, items);

    assertTrue(store.exists(testFlowId));

    store.delete(testFlowId);

    assertFalse(store.exists(testFlowId));
  }

  @Test
  @DisplayName("Should respect TTL")
  void testTTL() throws Exception {
    // Set short TTL for testing
    config.put("redis.ttl.seconds", "2");
    RedisShortTermStore shortTtlStore = new RedisShortTermStore();
    shortTtlStore.initialize(config);

    List<ContextItem> items = Arrays.asList(
        createTestItem("Message 1", ContextPriority.MUST)
    );
    shortTtlStore.putItems(testFlowId, items);

    assertTrue(shortTtlStore.exists(testFlowId));

    // Wait for TTL expiration
    Thread.sleep(3000);

    assertFalse(shortTtlStore.exists(testFlowId));

    shortTtlStore.close();
  }

  private ContextItem createTestItem(String content, ContextPriority priority) {
    ContextItem item = new ContextItem(content, priority, MemoryType.SHORT_TERM);
    item.setItemId(UUID.randomUUID().toString());
    return item;
  }
}
