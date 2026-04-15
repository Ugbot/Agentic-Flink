package com.ververica.flink.agent.storage.redis;

import static org.junit.jupiter.api.Assertions.*;

import com.ververica.flink.agent.context.core.ContextItem;
import com.ververica.flink.agent.context.core.ContextPriority;
import com.ververica.flink.agent.context.core.MemoryType;
import com.ververica.flink.agent.storage.StorageTier;
import java.util.*;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for RedisShortTermStore using Testcontainers.
 *
 * <p>These tests spin up a real Redis container via Testcontainers (works with Podman or Docker) and
 * exercise the full RedisShortTermStore against it. Tagged with "integration" so they are excluded
 * from default {@code mvn test} runs and only execute when the {@code integration-tests} profile is
 * active.
 *
 * <p>Run with: {@code mvn test -P integration-tests}
 *
 * @author Agentic Flink Team
 */
@Tag("integration")
@Testcontainers
class RedisShortTermStoreTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  private RedisShortTermStore store;
  private Map<String, String> config;
  private String testFlowId;

  @BeforeEach
  void setUp() throws Exception {
    config = new HashMap<>();
    config.put("redis.host", redis.getHost());
    config.put("redis.port", String.valueOf(redis.getMappedPort(6379)));
    config.put("redis.database", "0");
    config.put("redis.ttl.seconds", "300");

    store = new RedisShortTermStore();
    store.initialize(config);

    testFlowId = "test-flow-" + UUID.randomUUID();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (store != null) {
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
    List<ContextItem> items =
        Arrays.asList(
            createTestItem("Message 1", ContextPriority.MUST),
            createTestItem("Message 2", ContextPriority.SHOULD));

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
    List<ContextItem> items = Arrays.asList(createTestItem("Message 1", ContextPriority.MUST));
    store.putItems(testFlowId, items);

    assertTrue(store.exists(testFlowId));

    store.delete(testFlowId);

    assertFalse(store.exists(testFlowId));
  }

  @Test
  @DisplayName("Should respect TTL")
  void testTTL() throws Exception {
    // Set short TTL for testing
    Map<String, String> shortTtlConfig = new HashMap<>();
    shortTtlConfig.put("redis.host", redis.getHost());
    shortTtlConfig.put("redis.port", String.valueOf(redis.getMappedPort(6379)));
    shortTtlConfig.put("redis.database", "0");
    shortTtlConfig.put("redis.ttl.seconds", "2");

    RedisShortTermStore shortTtlStore = new RedisShortTermStore();
    shortTtlStore.initialize(shortTtlConfig);

    String ttlFlowId = "test-ttl-" + UUID.randomUUID();
    List<ContextItem> items = Arrays.asList(createTestItem("Message 1", ContextPriority.MUST));
    shortTtlStore.putItems(ttlFlowId, items);

    assertTrue(shortTtlStore.exists(ttlFlowId));

    // Wait for TTL expiration
    Thread.sleep(3000);

    assertFalse(shortTtlStore.exists(ttlFlowId));

    shortTtlStore.close();
  }

  private ContextItem createTestItem(String content, ContextPriority priority) {
    ContextItem item = new ContextItem(content, priority, MemoryType.SHORT_TERM);
    item.setItemId(UUID.randomUUID().toString());
    return item;
  }
}
