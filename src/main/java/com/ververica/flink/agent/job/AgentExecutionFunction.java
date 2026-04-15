package com.ververica.flink.agent.job;

import com.ververica.flink.agent.core.AgentEvent;
import com.ververica.flink.agent.core.AgentEventType;
import com.ververica.flink.agent.dsl.Agent;
import com.ververica.flink.agent.statemachine.AgentState;
import com.ververica.flink.agent.tool.ToolRegistry;
import java.util.List;
import java.util.Map;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.functions.TimedOutPartialMatchHandler;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CEP PatternProcessFunction for executing a single agent.
 *
 * <p>Processes pattern matches from the agent's state machine and performs:
 * <ul>
 *   <li>LLM calls with the agent's system prompt and tools</li>
 *   <li>Tool execution (async, with retries)</li>
 *   <li>Validation and correction loops</li>
 *   <li>Timeout handling and partial match recovery</li>
 *   <li>Side output routing for failures and timeouts</li>
 * </ul>
 *
 * <p>This function is invoked by Flink CEP when a pattern match occurs. The pattern
 * is generated from the agent's state machine.
 *
 * <p><b>State Flow:</b>
 * <pre>
 * INITIALIZED → VALIDATING → EXECUTING → [TOOL_CALLS] → COMPLETED
 *                    ↓              ↓
 *               CORRECTING    SUPERVISOR_REVIEW
 * </pre>
 *
 * @author Agentic Flink Team
 * @see Agent
 * @see AgentJobGenerator
 */
public class AgentExecutionFunction extends PatternProcessFunction<AgentEvent, AgentEvent>
    implements TimedOutPartialMatchHandler<AgentEvent> {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(AgentExecutionFunction.class);

  private final Agent agent;
  private final ToolRegistry toolRegistry;

  // Side output tags (from AgentJobGenerator)
  private static final OutputTag<AgentEvent> VALIDATION_FAILURES_TAG =
      AgentJobGenerator.VALIDATION_FAILURES_TAG;
  private static final OutputTag<AgentEvent> TIMEOUT_TAG =
      AgentJobGenerator.TIMEOUT_TAG;

  public AgentExecutionFunction(Agent agent, ToolRegistry toolRegistry) {
    this.agent = agent;
    this.toolRegistry = toolRegistry;
  }

  @Override
  public void processMatch(
      Map<String, List<AgentEvent>> match, Context ctx, Collector<AgentEvent> out)
      throws Exception {

    LOG.debug("Pattern match for agent: {}", agent.getAgentId());

    // Extract events from pattern match
    // Pattern structure depends on agent's state machine, but typically:
    // - "start" → FLOW_STARTED or INITIALIZED
    // - "validation" → VALIDATION_PASSED (if enabled)
    // - "execution" → TOOL_CALL_COMPLETED (may be multiple)
    // - "complete" → COMPLETED

    List<AgentEvent> startEvents = match.get("start");
    List<AgentEvent> executionEvents = match.get("execution");

    if (startEvents == null || startEvents.isEmpty()) {
      LOG.warn("No start event in pattern match for flow, skipping");
      return;
    }

    AgentEvent startEvent = startEvents.get(0);
    String flowId = startEvent.getFlowId();

    LOG.info("Processing agent execution for flow: {}", flowId);

    // Phase 3 (not yet implemented): Full agent execution logic
    // This is a placeholder that demonstrates the structure
    // Full implementation will include:
    // - LLM calls via LangChain4J
    // - Tool execution with async I/O
    // - Validation loops
    // - Correction loops
    // - Supervisor escalation checks

    try {
      // Simulate agent execution
      AgentEvent completionEvent = createCompletionEvent(startEvent, executionEvents);
      out.collect(completionEvent);

      LOG.info("Agent execution completed for flow: {}", flowId);

    } catch (Exception e) {
      LOG.error("Agent execution failed for flow: {}", flowId, e);

      // Create failure event
      AgentEvent failureEvent = startEvent.withEventType(AgentEventType.FLOW_FAILED);
      failureEvent.incrementIteration();
      failureEvent.putMetadata("state", AgentState.FAILED.name());
      failureEvent.getData().put("error", e.getMessage());

      // Route to validation failures side output
      ctx.output(VALIDATION_FAILURES_TAG, failureEvent);
    }
  }

  @Override
  public void processTimedOutMatch(
      Map<String, List<AgentEvent>> match, Context ctx) throws Exception {

    LOG.warn("Pattern match timed out for agent: {}", agent.getAgentId());

    List<AgentEvent> startEvents = match.get("start");
    if (startEvents == null || startEvents.isEmpty()) {
      return;
    }

    AgentEvent startEvent = startEvents.get(0);
    String flowId = startEvent.getFlowId();

    LOG.warn("Flow timed out: {}", flowId);

    // Create timeout event
    AgentEvent timeoutEvent = startEvent.withEventType(AgentEventType.TIMEOUT_OCCURRED);
    timeoutEvent.incrementIteration();
    timeoutEvent.putMetadata("state", AgentState.FAILED.name());
    timeoutEvent.getData().put("timeout_reason", "Pattern match exceeded time window");
    timeoutEvent.getData().put("agent_id", agent.getAgentId());

    // Route to timeout side output
    ctx.output(TIMEOUT_TAG, timeoutEvent);

    // If compensation is enabled, trigger compensation
    if (agent.isCompensationEnabled()) {
      AgentEvent compensationEvent = startEvent.createCompensationEvent();
      compensationEvent.putMetadata("state", AgentState.COMPENSATING.name());
      ctx.output(AgentJobGenerator.COMPENSATION_TAG, compensationEvent);
    }
  }

  /**
   * Creates a completion event from the execution results.
   *
   * <p>Phase 3 (not yet implemented): This will aggregate results from tool calls, validation, etc.
   */
  private AgentEvent createCompletionEvent(
      AgentEvent startEvent, List<AgentEvent> executionEvents) {

    AgentEvent completionEvent = startEvent.withEventType(AgentEventType.FLOW_COMPLETED);
    completionEvent.incrementIteration();
    completionEvent.putMetadata("state", AgentState.COMPLETED.name());

    // Phase 3 (not yet implemented): Aggregate execution results
    if (executionEvents != null && !executionEvents.isEmpty()) {
      completionEvent.getData().put("execution_count", executionEvents.size());
      completionEvent.getData().put("last_execution", executionEvents.get(executionEvents.size() - 1).getData());
    }

    completionEvent.getData().put("agent_id", agent.getAgentId());
    completionEvent.getData().put("completion_timestamp", System.currentTimeMillis());

    return completionEvent;
  }
}
