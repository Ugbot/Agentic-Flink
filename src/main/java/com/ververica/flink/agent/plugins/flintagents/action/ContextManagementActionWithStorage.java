package com.ververica.flink.agent.plugins.flintagents.action;

import com.ververica.flink.agent.context.core.AgentContext;
import com.ververica.flink.agent.context.core.ContextItem;
import com.ververica.flink.agent.context.core.ContextPriority;
import com.ververica.flink.agent.context.core.MemoryType;
import com.ververica.flink.agent.context.relevancy.RelevancyScorer;
import com.ververica.flink.agent.storage.LongTermMemoryStore;
import com.ververica.flink.agent.storage.ShortTermMemoryStore;
import com.ververica.flink.agent.storage.config.StorageConfiguration;
import com.ververica.flink.agent.storage.metrics.MetricsWrapper;
import com.ververica.flink.agent.storage.metrics.StorageMetrics;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.flink.agents.api.Event;
import org.apache.flink.agents.api.OutputEvent;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced stateful ProcessFunction with pluggable multi-tier storage.
 *
 * <p>This is an enhanced version of ContextManagementAction that integrates the pluggable storage
 * architecture, providing:
 *
 * <ul>
 *   <li>✅ Flink state backend for active processing (CHECKPOINT tier)
 *   <li>✅ ShortTermMemoryStore for hot-tier caching (HOT tier)
 *   <li>✅ LongTermMemoryStore for conversation persistence (WARM tier)
 *   <li>✅ MoSCoW prioritization and 5-phase compaction
 *   <li>✅ Conversation resumption after job restarts
 *   <li>✅ Storage metrics and monitoring
 * </ul>
 *
 * <p><b>Multi-Tier Storage Architecture:</b>
 *
 * <pre>{@code
 * CHECKPOINT (Flink State):  Active processing, sub-ms latency
 *     ↓
 * HOT (ShortTermMemoryStore): Recent context, <1ms latency, shared cache
 *     ↓
 * WARM (LongTermMemoryStore): Conversation persistence, 1-10ms, enables resumption
 * }</pre>
 *
 * <p><b>Hydration Pattern:</b>
 *
 * When processing the first event for a flowId:
 *
 * <ol>
 *   <li>Check Flink state (empty on first event)
 *   <li>Load from HOT tier cache (ShortTermMemoryStore)
 *   <li>If not in HOT, load from WARM tier (LongTermMemoryStore)
 *   <li>Populate Flink state with loaded context
 * </ol>
 *
 * <p><b>Persistence Pattern:</b>
 *
 * <ol>
 *   <li>All updates written to Flink state immediately
 *   <li>Periodically sync to HOT tier (every N events)
 *   <li>On compaction or timer, persist to WARM tier
 * </ol>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Create storage configuration
 * StorageConfiguration storageConfig = StorageConfiguration.builder()
 *     .withHotTier("redis", hotConfig)
 *     .withWarmTier("redis", warmConfig)
 *     .build();
 *
 * // Create action with storage
 * ContextManagementActionWithStorage action =
 *     new ContextManagementActionWithStorage("agent-001", storageConfig);
 *
 * // Use in Flink job
 * DataStream<Event> processed = events
 *     .keyBy(event -> event.getAttr("flowId"))
 *     .process(action);
 * }</pre>
 *
 * @author Agentic Flink Team
 */
public class ContextManagementActionWithStorage
    extends KeyedProcessFunction<String, Event, Event> {

  private static final Logger LOG =
      LoggerFactory.getLogger(ContextManagementActionWithStorage.class);

  private final String agentId;
  private final int maxTokens;
  private final int maxItems;
  private final double compactionThreshold;
  private final double relevancyThreshold;
  private final double longTermPromotionThreshold;
  private final StorageConfiguration storageConfig;

  // Persistence control
  private final int hotTierSyncInterval; // Sync to HOT tier every N events
  private final int warmTierSyncInterval; // Sync to WARM tier every N events

  // Flink State - CHECKPOINT tier (active processing)
  private transient ValueState<AgentContext> contextState;
  private transient MapState<String, ContextItem> activeItemsState;
  private transient ValueState<Integer> eventCountState; // Track events for sync intervals

  // Pluggable Storage - HOT and WARM tiers
  private transient ShortTermMemoryStore hotStore;
  private transient LongTermMemoryStore warmStore;

  // Metrics
  private transient StorageMetrics hotMetrics;
  private transient StorageMetrics warmMetrics;

  // Relevancy scorer
  private transient RelevancyScorer relevancyScorer;

  /**
   * Create action with storage configuration.
   *
   * @param agentId Unique identifier for this agent
   * @param storageConfig Storage configuration for HOT and WARM tiers
   */
  public ContextManagementActionWithStorage(String agentId, StorageConfiguration storageConfig) {
    this(agentId, storageConfig, 4000, 50, 0.8, 0.5, 0.7, 10, 50);
  }

  /**
   * Create action with full configuration.
   *
   * @param agentId Unique identifier
   * @param storageConfig Storage configuration
   * @param maxTokens Maximum tokens in context
   * @param maxItems Maximum items in context
   * @param compactionThreshold Trigger compaction at usage ratio
   * @param relevancyThreshold Minimum relevancy to keep
   * @param longTermPromotionThreshold Minimum score for promotion
   * @param hotTierSyncInterval Sync to HOT tier every N events
   * @param warmTierSyncInterval Sync to WARM tier every N events
   */
  public ContextManagementActionWithStorage(
      String agentId,
      StorageConfiguration storageConfig,
      int maxTokens,
      int maxItems,
      double compactionThreshold,
      double relevancyThreshold,
      double longTermPromotionThreshold,
      int hotTierSyncInterval,
      int warmTierSyncInterval) {
    this.agentId = agentId;
    this.storageConfig = storageConfig;
    this.maxTokens = maxTokens;
    this.maxItems = maxItems;
    this.compactionThreshold = compactionThreshold;
    this.relevancyThreshold = relevancyThreshold;
    this.longTermPromotionThreshold = longTermPromotionThreshold;
    this.hotTierSyncInterval = hotTierSyncInterval;
    this.warmTierSyncInterval = warmTierSyncInterval;
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);

    // Initialize Flink state
    ValueStateDescriptor<AgentContext> contextDescriptor =
        new ValueStateDescriptor<>("agentContext", AgentContext.class);
    contextState = getRuntimeContext().getState(contextDescriptor);

    MapStateDescriptor<String, ContextItem> itemsDescriptor =
        new MapStateDescriptor<>("activeItems", String.class, ContextItem.class);
    activeItemsState = getRuntimeContext().getMapState(itemsDescriptor);

    ValueStateDescriptor<Integer> eventCountDescriptor =
        new ValueStateDescriptor<>("eventCount", Integer.class);
    eventCountState = getRuntimeContext().getState(eventCountDescriptor);

    // Initialize pluggable storage
    if (storageConfig.isTierConfigured(com.ververica.flink.agent.storage.StorageTier.HOT)) {
      ShortTermMemoryStore store = storageConfig.createShortTermStore();
      MetricsWrapper<String, List<ContextItem>> metricsStore = new MetricsWrapper<>(store);
      this.hotStore = (ShortTermMemoryStore) metricsStore;
      this.hotMetrics = metricsStore.getMetrics();
      LOG.info("HOT tier storage initialized: {}", store.getProviderName());
    } else {
      LOG.warn("HOT tier not configured - using Flink state only");
    }

    if (storageConfig.isTierConfigured(com.ververica.flink.agent.storage.StorageTier.WARM)) {
      LongTermMemoryStore store = storageConfig.createLongTermStore();
      MetricsWrapper<String, AgentContext> metricsStore = new MetricsWrapper<>(store);
      this.warmStore = (LongTermMemoryStore) metricsStore;
      this.warmMetrics = metricsStore.getMetrics();
      LOG.info("WARM tier storage initialized: {}", store.getProviderName());
    } else {
      LOG.warn("WARM tier not configured - conversation resumption disabled");
    }

    // Initialize relevancy scorer
    Map<String, String> scorerConfig = new HashMap<>();
    scorerConfig.put("baseUrl", "http://localhost:11434");
    scorerConfig.put("modelName", "nomic-embed-text:latest");
    this.relevancyScorer = new RelevancyScorer(scorerConfig);

    LOG.info(
        "ContextManagementActionWithStorage initialized: agent={}, maxTokens={}, maxItems={}, "
            + "hotTierSync={}, warmTierSync={}",
        agentId, maxTokens, maxItems, hotTierSyncInterval, warmTierSyncInterval);
  }

  @Override
  public void processElement(Event event, Context ctx, Collector<Event> out) throws Exception {
    String flowId = (String) event.getAttr("flowId");
    if (flowId == null) {
      flowId = "unknown";
    }

    // Get or hydrate context
    AgentContext context = contextState.value();
    if (context == null) {
      context = hydrateContext(flowId, event);
      LOG.info("Hydrated context for flow {}", flowId);
    }

    // Extract message content
    Object messageContent = event.getAttributes().get("message");
    if (messageContent == null) {
      out.collect(event);
      return;
    }

    // Create context item
    ContextItem item =
        new ContextItem(
            messageContent.toString(), ContextPriority.SHOULD, MemoryType.SHORT_TERM);

    // Add to active items state
    activeItemsState.put(item.getItemId(), item);

    // Update context
    context.updateLastAccess();
    contextState.update(context);

    // Increment event count and check sync intervals
    Integer eventCount = eventCountState.value();
    if (eventCount == null) {
      eventCount = 0;
    }
    eventCount++;
    eventCountState.update(eventCount);

    // Sync to HOT tier
    if (hotStore != null && eventCount % hotTierSyncInterval == 0) {
      syncToHotTier(flowId);
    }

    // Sync to WARM tier
    if (warmStore != null && eventCount % warmTierSyncInterval == 0) {
      syncToWarmTier(flowId, context);
    }

    // Check for compaction
    int currentTokens = calculateTotalTokens();
    int currentItems = countItems();
    double usageRatio = (double) currentTokens / maxTokens;

    if (usageRatio >= compactionThreshold || currentItems >= maxItems) {
      LOG.info(
          "Context overflow for flow {}: usage={:.1f}%, triggering compaction",
          flowId, usageRatio * 100);

      performCompaction(event, ctx, out, context, flowId);

      // Persist after compaction
      if (warmStore != null) {
        syncToWarmTier(flowId, context);
      }
    }

    out.collect(event);
  }

  /**
   * Hydrate context from storage tiers.
   *
   * <p>Tries HOT tier first, then WARM tier. If not found in either, creates new context.
   *
   * @param flowId Conversation flow identifier
   * @param event Current event
   * @return Hydrated or new context
   */
  private AgentContext hydrateContext(String flowId, Event event) throws Exception {
    // Try HOT tier first
    if (hotStore != null) {
      List<ContextItem> hotItems = hotStore.getItems(flowId);
      if (!hotItems.isEmpty()) {
        LOG.debug("Hydrated {} items from HOT tier for flow {}", hotItems.size(), flowId);
        // Populate Flink state
        for (ContextItem item : hotItems) {
          activeItemsState.put(item.getItemId(), item);
        }
      }
    }

    // Try WARM tier for full context
    if (warmStore != null) {
      Optional<AgentContext> loadedContext = warmStore.loadContext(flowId);
      if (loadedContext.isPresent()) {
        LOG.info("Hydrated context from WARM tier for flow {}", flowId);
        AgentContext context = loadedContext.get();

        // Load long-term facts
        Map<String, ContextItem> facts = warmStore.loadFacts(flowId);
        for (ContextItem fact : facts.values()) {
          activeItemsState.put(fact.getItemId(), fact);
        }

        return context;
      }
    }

    // Create new context if not found
    LOG.info("Creating new context for flow {}", flowId);
    String userId = (String) event.getAttr("userId");
    return new AgentContext(agentId, flowId, userId, maxTokens, maxItems);
  }

  /** Sync active items to HOT tier. */
  private void syncToHotTier(String flowId) throws Exception {
    if (hotStore == null) return;

    List<ContextItem> items = new ArrayList<>();
    for (Map.Entry<String, ContextItem> entry : activeItemsState.entries()) {
      items.add(entry.getValue());
    }

    hotStore.putItems(flowId, items);
    LOG.debug("Synced {} items to HOT tier for flow {}", items.size(), flowId);
  }

  /** Sync context and facts to WARM tier. */
  private void syncToWarmTier(String flowId, AgentContext context) throws Exception {
    if (warmStore == null) return;

    // Save context
    warmStore.saveContext(flowId, context);

    // Save facts (items marked as long-term)
    Map<String, ContextItem> facts = new HashMap<>();
    for (Map.Entry<String, ContextItem> entry : activeItemsState.entries()) {
      ContextItem item = entry.getValue();
      if (item.getMemoryType() == MemoryType.LONG_TERM) {
        facts.put(item.getItemId(), item);
      }
    }

    if (!facts.isEmpty()) {
      warmStore.saveFacts(flowId, facts);
    }

    LOG.debug("Synced context and {} facts to WARM tier for flow {}", facts.size(), flowId);
  }

  /** Perform MoSCoW compaction. */
  private void performCompaction(
      Event event, Context ctx, Collector<Event> out, AgentContext context, String flowId)
      throws Exception {

    long startTime = System.currentTimeMillis();
    int originalTokens = calculateTotalTokens();
    int originalItems = countItems();

    // Get all items
    List<ContextItem> items = new ArrayList<>();
    for (Map.Entry<String, ContextItem> entry : activeItemsState.entries()) {
      items.add(entry.getValue());
    }

    String currentIntent = (String) event.getAttr("currentIntent");

    // Phase 1: Remove WONT items
    List<ContextItem> wontItems =
        items.stream()
            .filter(item -> item.getPriority() == ContextPriority.WONT)
            .collect(Collectors.toList());
    items.removeAll(wontItems);
    int removedCount = wontItems.size();

    // Phase 2: Score relevancy
    Map<String, Double> scores = new HashMap<>();
    for (ContextItem item : items) {
      double score = relevancyScorer.scoreRelevancy(item, currentIntent).get();
      scores.put(item.getItemId(), score);
    }

    // Phase 3: Remove low-relevancy COULD items
    List<ContextItem> couldItems =
        items.stream()
            .filter(item -> item.getPriority() == ContextPriority.COULD)
            .collect(Collectors.toList());
    List<ContextItem> lowRelevancyCould = new ArrayList<>();
    for (ContextItem item : couldItems) {
      if (scores.getOrDefault(item.getItemId(), 0.0) < relevancyThreshold) {
        lowRelevancyCould.add(item);
      }
    }
    items.removeAll(lowRelevancyCould);
    removedCount += lowRelevancyCould.size();

    // Phase 4: Compress SHOULD items if needed
    int compressedCount = 0;
    if (calculateTokensForItems(items) > maxTokens * compactionThreshold) {
      List<ContextItem> shouldItems =
          items.stream()
              .filter(item -> item.getPriority() == ContextPriority.SHOULD)
              .sorted(
                  (a, b) ->
                      Double.compare(
                          scores.getOrDefault(b.getItemId(), 0.0),
                          scores.getOrDefault(a.getItemId(), 0.0)))
              .collect(Collectors.toList());

      int toRemove = shouldItems.size() / 2;
      List<ContextItem> toCompact =
          shouldItems.subList(
              Math.max(0, shouldItems.size() - toRemove), shouldItems.size());
      items.removeAll(toCompact);
      compressedCount = toCompact.size();
    }

    // Phase 5: Promote to long-term
    int promotedCount = 0;
    for (ContextItem item : items) {
      double score = scores.getOrDefault(item.getItemId(), 0.0);
      if (score >= longTermPromotionThreshold && item.getPriority() == ContextPriority.MUST) {
        item.setMemoryType(MemoryType.LONG_TERM);
        promotedCount++;
      }
    }

    // Update state with compacted items
    activeItemsState.clear();
    for (ContextItem item : items) {
      activeItemsState.put(item.getItemId(), item);
    }

    int compactedTokens = calculateTotalTokens();
    long compactionTimeMs = System.currentTimeMillis() - startTime;

    LOG.info(
        "Compaction complete for flow {}: {}→{} tokens, {}→{} items, removed={}, "
            + "compressed={}, promoted={}, time={}ms",
        flowId,
        originalTokens,
        compactedTokens,
        originalItems,
        items.size(),
        removedCount,
        compressedCount,
        promotedCount,
        compactionTimeMs);

    // Emit compaction event
    Map<String, Object> data = new HashMap<>();
    data.put("eventType", "ContextCompacted");
    data.put("originalTokens", originalTokens);
    data.put("compactedTokens", compactedTokens);
    data.put("tokensSaved", originalTokens - compactedTokens);
    data.put("removedCount", removedCount);
    data.put("compressedCount", compressedCount);
    data.put("promotedCount", promotedCount);
    data.put("compactionTimeMs", compactionTimeMs);

    OutputEvent compactionEvent = new OutputEvent(data);
    compactionEvent.setAttr("agentId", agentId);
    compactionEvent.setAttr("flowId", flowId);
    compactionEvent.setSourceTimestamp(System.currentTimeMillis());
    out.collect(compactionEvent);
  }

  private int calculateTotalTokens() throws Exception {
    int total = 0;
    for (Map.Entry<String, ContextItem> entry : activeItemsState.entries()) {
      total += entry.getValue().getTokenCount();
    }
    return total;
  }

  @SuppressWarnings("unused")
  private int countItems() throws Exception {
    int count = 0;
    for (String ignored : activeItemsState.keys()) {
      count++;
    }
    return count;
  }

  private int calculateTokensForItems(List<ContextItem> items) {
    return items.stream().mapToInt(ContextItem::getTokenCount).sum();
  }

  @Override
  public void close() throws Exception {
    if (hotStore != null) {
      hotStore.close();
      if (hotMetrics != null) {
        LOG.info("HOT tier metrics: {}", hotMetrics);
      }
    }
    if (warmStore != null) {
      warmStore.close();
      if (warmMetrics != null) {
        LOG.info("WARM tier metrics: {}", warmMetrics);
      }
    }
    super.close();
  }

  public String getAgentId() {
    return agentId;
  }

  public StorageMetrics getHotMetrics() {
    return hotMetrics;
  }

  public StorageMetrics getWarmMetrics() {
    return warmMetrics;
  }
}
