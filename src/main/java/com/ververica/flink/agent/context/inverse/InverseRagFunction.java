package com.ververica.flink.agent.context.inverse;

import com.ververica.flink.agent.context.compaction.CompactionResult;
import com.ververica.flink.agent.context.core.ContextItem;
import com.ververica.flink.agent.tools.rag.DocumentIngestionToolExecutor;
import java.util.HashMap;
import java.util.Map;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inverse RAG: Pushes compacted context to long-term storage
 *
 * <p>Instead of retrieving (RAG), we store (Inverse RAG) Takes high-relevancy compacted items and
 * pushes to vector store for future retrieval
 */
public class InverseRagFunction
    extends ProcessFunction<CompactionResult, InverseRagResult> {

  private static final Logger LOG = LoggerFactory.getLogger(InverseRagFunction.class);
  public static final String UID = InverseRagFunction.class.getSimpleName();

  private transient DocumentIngestionToolExecutor ingestionExecutor;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);

    // Initialize document ingestion tool
    Map<String, String> config = new HashMap<>();
    config.put("baseUrl", "http://localhost:11434");
    config.put("modelName", "nomic-embed-text:latest");
    config.put("host", "localhost");
    config.put("port", "6333");
    config.put("collectionName", "agent-long-term-memory");

    this.ingestionExecutor = new DocumentIngestionToolExecutor(config);
  }

  @Override
  public void processElement(
      CompactionResult compactionResult,
      Context ctx,
      Collector<InverseRagResult> out)
      throws Exception {

    LOG.info(
        "Starting inverse RAG for flow {}, promoting {} items to long-term",
        compactionResult.getFlowId(),
        compactionResult.getPromotedToLongTerm().size());

    InverseRagResult result =
        new InverseRagResult(compactionResult.getRequestId(), compactionResult.getFlowId());

    // Push each promoted item to vector store
    for (ContextItem item : compactionResult.getPromotedToLongTerm()) {
      try {
        Map<String, Object> params = new HashMap<>();
        params.put("content", item.getContent());
        params.put("chunk_size", 500);
        params.put("chunk_overlap", 50);

        // Add metadata for retrieval
        Map<String, String> metadata = new HashMap<>();
        metadata.put("item_id", item.getItemId());
        metadata.put("priority", item.getPriority().name());
        metadata.put("intent_tag", item.getIntentTag());
        metadata.put("created_at", String.valueOf(item.getCreatedAt()));
        metadata.put("agent_id", compactionResult.getCompactedContext().getAgentId());
        metadata.put("flow_id", compactionResult.getFlowId());
        params.put("metadata", metadata);

        // Execute ingestion
        Object ingestionResult = ingestionExecutor.execute(params).get();

        result.addStoredItem(item.getItemId(), ingestionResult.toString());
        LOG.debug(
            "Stored item {} to long-term memory: {}", item.getItemId(), ingestionResult);

      } catch (Exception e) {
        LOG.error("Failed to store item {} to long-term memory", item.getItemId(), e);
        result.addFailedItem(item.getItemId(), e.getMessage());
      }
    }

    LOG.info(
        "Inverse RAG complete for flow {}: stored={}, failed={}",
        compactionResult.getFlowId(),
        result.getStoredCount(),
        result.getFailedCount());

    out.collect(result);
  }
}
