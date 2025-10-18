package com.ververica.flink.agent.state;

import com.ververica.flink.agent.context.core.AgentContext;
import com.ververica.flink.agent.context.core.ContextItem;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pluggable external state storage for conversation persistence and resumption.
 *
 * <p>This interface allows agent conversations to be persisted to external storage systems
 * (Redis, DynamoDB, PostgreSQL, etc.) so they can be resumed across job restarts, scaled
 * independently, or accessed by multiple systems.
 *
 * <p><b>Use Cases:</b>
 *
 * <ul>
 *   <li>✅ Resume conversations after job restart
 *   <li>✅ Share context between multiple Flink jobs
 *   <li>✅ Query conversation history from external systems
 *   <li>✅ Long-term conversation storage (beyond Flink checkpoints)
 *   <li>✅ Multi-tenant conversation isolation
 * </ul>
 *
 * <p><b>Architecture Pattern:</b>
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────┐
 * │  Flink State Backend (RocksDB)                           │
 * │  • Active processing state                               │
 * │  • Fault tolerance via checkpoints                       │
 * │  • Fast access during processing                         │
 * └─────────────────────┬────────────────────────────────────┘
 *                       │
 *                       │ Periodically persist
 *                       ↓
 * ┌──────────────────────────────────────────────────────────┐
 * │  External State Store (Redis/DynamoDB/PostgreSQL)        │
 * │  • Long-term conversation persistence                    │
 * │  • Cross-job sharing                                     │
 * │  • Query and analytics                                   │
 * │  • Resume conversations                                  │
 * └──────────────────────────────────────────────────────────┘
 *                       │
 *                       │ Hydrate on startup
 *                       ↑
 * ┌──────────────────────────────────────────────────────────┐
 * │  Context Hydration                                       │
 * │  • Load context when conversation resumes                │
 * │  • Populate Flink state from external store              │
 * └──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>Implementation Examples:</b>
 *
 * <ul>
 *   <li>{@link RedisStateStore} - Fast, in-memory cache
 *   <li>{@link DynamoDBStateStore} - Serverless, scalable
 *   <li>{@link PostgreSQLStateStore} - Relational, queryable
 * </ul>
 *
 * @author Agentic Flink Team
 * @see AgentContext
 * @see ContextItem
 */
public interface ExternalStateStore extends Serializable {

  // ===================================================================
  // CONVERSATION-LEVEL OPERATIONS
  // ===================================================================

  /**
   * Saves complete agent context to external storage.
   *
   * @param flowId Unique conversation/flow identifier
   * @param context Agent context to persist
   * @throws Exception if save fails
   */
  void saveContext(String flowId, AgentContext context) throws Exception;

  /**
   * Loads agent context from external storage.
   *
   * @param flowId Unique conversation/flow identifier
   * @return Optional containing context if found, empty otherwise
   * @throws Exception if load fails
   */
  Optional<AgentContext> loadContext(String flowId) throws Exception;

  /**
   * Checks if conversation exists in storage.
   *
   * @param flowId Unique conversation/flow identifier
   * @return true if conversation exists
   * @throws Exception if check fails
   */
  boolean conversationExists(String flowId) throws Exception;

  /**
   * Deletes conversation from storage.
   *
   * @param flowId Unique conversation/flow identifier
   * @throws Exception if delete fails
   */
  void deleteConversation(String flowId) throws Exception;

  // ===================================================================
  // CONTEXT ITEM OPERATIONS
  // ===================================================================

  /**
   * Saves individual context items (short-term memory).
   *
   * @param flowId Unique conversation/flow identifier
   * @param items List of context items to save
   * @throws Exception if save fails
   */
  void saveContextItems(String flowId, List<ContextItem> items) throws Exception;

  /**
   * Loads context items for a conversation.
   *
   * @param flowId Unique conversation/flow identifier
   * @return List of context items
   * @throws Exception if load fails
   */
  List<ContextItem> loadContextItems(String flowId) throws Exception;

  /**
   * Saves long-term memory facts.
   *
   * @param flowId Unique conversation/flow identifier
   * @param facts Map of fact ID to context item
   * @throws Exception if save fails
   */
  void saveLongTermFacts(String flowId, Map<String, ContextItem> facts) throws Exception;

  /**
   * Loads long-term memory facts.
   *
   * @param flowId Unique conversation/flow identifier
   * @return Map of fact ID to context item
   * @throws Exception if load fails
   */
  Map<String, ContextItem> loadLongTermFacts(String flowId) throws Exception;

  // ===================================================================
  // METADATA OPERATIONS
  // ===================================================================

  /**
   * Lists all active conversations.
   *
   * @return List of flow IDs
   * @throws Exception if listing fails
   */
  List<String> listActiveConversations() throws Exception;

  /**
   * Lists conversations for a specific user.
   *
   * @param userId User identifier
   * @return List of flow IDs
   * @throws Exception if listing fails
   */
  List<String> listConversationsForUser(String userId) throws Exception;

  /**
   * Gets metadata about a conversation without loading full context.
   *
   * @param flowId Unique conversation/flow identifier
   * @return Metadata map (created_at, last_updated, item_count, etc.)
   * @throws Exception if retrieval fails
   */
  Map<String, Object> getConversationMetadata(String flowId) throws Exception;

  // ===================================================================
  // LIFECYCLE
  // ===================================================================

  /**
   * Initializes the state store (connection, schema creation, etc.).
   *
   * @param config Configuration parameters
   * @throws Exception if initialization fails
   */
  void initialize(Map<String, String> config) throws Exception;

  /**
   * Closes connections and releases resources.
   *
   * @throws Exception if close fails
   */
  void close() throws Exception;

  // ===================================================================
  // ADVANCED OPERATIONS
  // ===================================================================

  /**
   * Saves context asynchronously (non-blocking).
   *
   * <p>Default implementation is synchronous. Override for async support.
   *
   * @param flowId Unique conversation/flow identifier
   * @param context Agent context to persist
   */
  default void saveContextAsync(String flowId, AgentContext context) {
    try {
      saveContext(flowId, context);
    } catch (Exception e) {
      throw new RuntimeException("Async save failed", e);
    }
  }

  /**
   * Bulk saves multiple conversations (for batch processing).
   *
   * @param contexts Map of flow ID to context
   * @throws Exception if bulk save fails
   */
  default void bulkSave(Map<String, AgentContext> contexts) throws Exception {
    for (Map.Entry<String, AgentContext> entry : contexts.entrySet()) {
      saveContext(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Archives old conversations (move to cold storage).
   *
   * @param flowId Unique conversation/flow identifier
   * @param archiveLocation Archive location identifier
   * @throws Exception if archive fails
   */
  default void archiveConversation(String flowId, String archiveLocation) throws Exception {
    throw new UnsupportedOperationException("Archive not implemented");
  }
}
