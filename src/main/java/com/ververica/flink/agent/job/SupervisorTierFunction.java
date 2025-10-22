package com.ververica.flink.agent.job;

import com.ververica.flink.agent.core.AgentEvent;
import com.ververica.flink.agent.core.AgentEventType;
import com.ververica.flink.agent.dsl.Agent;
import com.ververica.flink.agent.dsl.SupervisorChain;
import com.ververica.flink.agent.dsl.SupervisorChain.EscalationPolicy;
import com.ververica.flink.agent.dsl.SupervisorChain.SupervisorTier;
import com.ververica.flink.agent.statemachine.AgentState;
import com.ververica.flink.agent.tool.ToolRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.functions.TimedOutPartialMatchHandler;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CEP PatternProcessFunction for executing a supervisor tier with escalation logic.
 *
 * <p>Extends agent execution with supervisor-specific capabilities:
 * <ul>
 *   <li>Quality score evaluation against tier thresholds</li>
 *   <li>Escalation to next tier based on quality/failure</li>
 *   <li>Escalation policy enforcement (NEXT_TIER, SKIP_TO_TOP, RETRY_CURRENT, FAIL_FAST)</li>
 *   <li>Human approval handling (pause/resume)</li>
 *   <li>Max escalation limits</li>
 * </ul>
 *
 * <p><b>Escalation Flow Example (NEXT_TIER policy):</b>
 * <pre>
 * Tier 0 (Executor) → Quality < 0.7? → Escalate to Tier 1 (QA Review)
 * Tier 1 (QA Review) → Quality < 0.8? → Escalate to Tier 2 (Final Approval)
 * Tier 2 (Final Approval) → Human Approval → COMPLETED or ESCALATE
 * </pre>
 *
 * <p><b>Escalation Metadata:</b>
 * <pre>
 * event.metadata:
 *   - current_tier: 0
 *   - target_tier: 1
 *   - escalation_count: 1
 *   - escalation_reason: "quality_threshold"
 *   - quality_score: 0.65
 * </pre>
 *
 * @author Agentic Flink Team
 * @see SupervisorChain
 * @see EscalationPolicy
 */
public class SupervisorTierFunction extends PatternProcessFunction<AgentEvent, AgentEvent>
    implements TimedOutPartialMatchHandler<AgentEvent> {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(SupervisorTierFunction.class);

  private final SupervisorTier tier;
  private final SupervisorChain chain;
  private final ToolRegistry toolRegistry;

  // Side output tags
  private static final OutputTag<AgentEvent> ESCALATION_TAG =
      AgentJobGenerator.ESCALATION_TAG;
  private static final OutputTag<AgentEvent> TIMEOUT_TAG =
      AgentJobGenerator.TIMEOUT_TAG;
  private static final OutputTag<AgentEvent> VALIDATION_FAILURES_TAG =
      AgentJobGenerator.VALIDATION_FAILURES_TAG;

  public SupervisorTierFunction(
      SupervisorTier tier, SupervisorChain chain, ToolRegistry toolRegistry) {
    this.tier = tier;
    this.chain = chain;
    this.toolRegistry = toolRegistry;
  }

  @Override
  public void processMatch(
      Map<String, List<AgentEvent>> match, Context ctx, Collector<AgentEvent> out)
      throws Exception {

    Agent agent = tier.getAgent();
    LOG.debug("Processing tier {} ({}) for agent: {}",
        tier.getTierIndex(), tier.getTierName(), agent.getAgentId());

    List<AgentEvent> startEvents = match.get("start");
    if (startEvents == null || startEvents.isEmpty()) {
      LOG.warn("No start event in pattern match, skipping");
      return;
    }

    AgentEvent startEvent = startEvents.get(0);
    String flowId = startEvent.getFlowId();

    // Get escalation count from metadata
    int escalationCount = getEscalationCount(startEvent);

    LOG.info("Processing tier {} for flow: {} (escalation count: {})",
        tier.getTierIndex(), flowId, escalationCount);

    // Check max escalations
    if (escalationCount >= chain.getMaxEscalations()) {
      if (chain.isFailOnMaxEscalations()) {
        LOG.error("Max escalations ({}) reached for flow: {}, failing",
            chain.getMaxEscalations(), flowId);
        handleMaxEscalationsReached(startEvent, ctx, out);
        return;
      } else {
        LOG.warn("Max escalations ({}) reached for flow: {}, allowing completion",
            chain.getMaxEscalations(), flowId);
        // Continue processing at final tier
      }
    }

    try {
      // TODO: Phase 3 - Implement full supervisor tier execution
      // This is a placeholder showing the escalation logic structure

      // Simulate tier execution
      AgentEvent tierResult = executeTier(startEvent, match);

      // Evaluate quality and determine if escalation is needed
      double qualityScore = evaluateQualityScore(tierResult);
      tierResult.getData().put("quality_score", qualityScore);
      tierResult.getData().put("tier_index", tier.getTierIndex());
      tierResult.getData().put("tier_name", tier.getTierName());

      LOG.debug("Tier {} quality score: {} (threshold: {})",
          tier.getTierIndex(), qualityScore, tier.getQualityThreshold());

      // Check if escalation is needed
      if (shouldEscalate(qualityScore)) {
        handleEscalation(tierResult, escalationCount, ctx, out);
      } else {
        // Quality passed - emit completion event
        AgentEvent completionEvent = tierResult.withEventType(AgentEventType.FLOW_COMPLETED);
        completionEvent.putMetadata("state", AgentState.COMPLETED.name());
        completionEvent.getData().put("approved_by_tier", tier.getTierIndex());

        out.collect(completionEvent);
        LOG.info("Tier {} approved flow: {}", tier.getTierIndex(), flowId);
      }

    } catch (Exception e) {
      LOG.error("Tier {} execution failed for flow: {}", tier.getTierIndex(), flowId, e);

      AgentEvent failureEvent = startEvent.withEventType(AgentEventType.FLOW_FAILED);
      failureEvent.incrementIteration();
      failureEvent.putMetadata("state", AgentState.FAILED.name());
      failureEvent.getData().put("error", e.getMessage());
      failureEvent.getData().put("failed_at_tier", tier.getTierIndex());

      ctx.output(VALIDATION_FAILURES_TAG, failureEvent);
    }
  }

  /**
   * Executes this tier (LLM call + tool execution).
   *
   * <p>TODO: Phase 3 - Implement full execution with LangChain4J.
   */
  private AgentEvent executeTier(AgentEvent startEvent, Map<String, List<AgentEvent>> match) {
    // Placeholder - will call LLM with tier agent's prompt and tools
    AgentEvent result = startEvent.withEventType(AgentEventType.SUPERVISOR_REVIEW_COMPLETED);
    result.incrementIteration();
    result.putMetadata("state", AgentState.SUPERVISOR_REVIEW.name());

    result.getData().put("reviewed_by", tier.getAgent().getAgentId());
    result.getData().put("tier_index", tier.getTierIndex());

    return result;
  }

  /**
   * Evaluates quality score from tier execution.
   *
   * <p>TODO: Phase 3 - Implement quality evaluation (LLM-based or rule-based).
   */
  private double evaluateQualityScore(AgentEvent tierResult) {
    // Placeholder - will evaluate based on LLM output or validation rules
    // For now, return a simulated score
    Object scoreObj = tierResult.getData().get("quality_score");
    if (scoreObj instanceof Number) {
      return ((Number) scoreObj).doubleValue();
    }

    // Default: assume quality is good if no explicit score
    return 0.85;
  }

  /**
   * Checks if escalation should occur based on quality score and tier config.
   */
  private boolean shouldEscalate(double qualityScore) {
    // Check tier-specific threshold
    if (tier.getQualityThreshold() > 0 && qualityScore < tier.getQualityThreshold()) {
      return true;
    }

    // Check chain-level threshold
    if (chain.shouldEscalate(qualityScore)) {
      return true;
    }

    // Check if human approval required (treated as escalation to human)
    if (tier.isRequiresHumanApproval()) {
      // TODO: Phase 3 - Implement human approval logic
      LOG.info("Tier {} requires human approval", tier.getTierIndex());
      return false;  // For now, don't escalate (assume approved)
    }

    return false;
  }

  /**
   * Handles escalation to next tier based on escalation policy.
   */
  private void handleEscalation(
      AgentEvent tierResult,
      int currentEscalationCount,
      Context ctx,
      Collector<AgentEvent> out) {

    int newEscalationCount = currentEscalationCount + 1;
    EscalationPolicy policy = chain.getEscalationPolicy();

    LOG.info("Escalating flow {} from tier {} (policy: {}, escalation count: {})",
        tierResult.getFlowId(), tier.getTierIndex(), policy, newEscalationCount);

    AgentEvent escalationEvent = tierResult.withEventType(AgentEventType.SUPERVISOR_REVIEW_REJECTED);
    escalationEvent.incrementIteration();
    escalationEvent.putMetadata("state", AgentState.SUPERVISOR_REVIEW.name());

    // Set escalation metadata
    escalationEvent.putMetadata("escalation_count", newEscalationCount);
    escalationEvent.putMetadata("escalation_reason", "quality_threshold");
    escalationEvent.putMetadata("escalated_from_tier", tier.getTierIndex());

    // Determine target tier based on policy
    int targetTier = determineTargetTier(policy);
    escalationEvent.putMetadata("target_tier", targetTier);

    // Route to escalation side output (will be picked up by next tier)
    ctx.output(ESCALATION_TAG, escalationEvent);

    LOG.info("Escalated flow {} to tier {}", tierResult.getFlowId(), targetTier);
  }

  /**
   * Determines target tier based on escalation policy.
   */
  private int determineTargetTier(EscalationPolicy policy) {
    switch (policy) {
      case NEXT_TIER:
        // Escalate to next tier in sequence
        Optional<SupervisorTier> nextTier = chain.getNextTier(tier.getTierIndex());
        return nextTier.map(SupervisorTier::getTierIndex).orElse(tier.getTierIndex());

      case SKIP_TO_TOP:
        // Skip to final tier
        return chain.getLastTier().getTierIndex();

      case RETRY_CURRENT:
        // Stay at current tier (retry)
        return tier.getTierIndex();

      case FAIL_FAST:
        // No escalation (will fail)
        return -1;

      case CUSTOM:
        // TODO: Phase 3 - Implement custom escalation logic
        LOG.warn("Custom escalation policy not yet implemented, using NEXT_TIER");
        return chain.getNextTier(tier.getTierIndex())
            .map(SupervisorTier::getTierIndex)
            .orElse(tier.getTierIndex());

      default:
        return chain.getNextTier(tier.getTierIndex())
            .map(SupervisorTier::getTierIndex)
            .orElse(tier.getTierIndex());
    }
  }

  /**
   * Handles max escalations reached.
   */
  private void handleMaxEscalationsReached(
      AgentEvent event, Context ctx, Collector<AgentEvent> out) {

    AgentEvent failureEvent = event.withEventType(AgentEventType.FLOW_FAILED);
    failureEvent.incrementIteration();
    failureEvent.putMetadata("state", AgentState.FAILED.name());

    failureEvent.getData().put("error", "Max escalations reached");
    failureEvent.getData().put("max_escalations", chain.getMaxEscalations());
    failureEvent.getData().put("final_tier", tier.getTierIndex());

    ctx.output(VALIDATION_FAILURES_TAG, failureEvent);
  }

  /**
   * Gets escalation count from event metadata.
   */
  private int getEscalationCount(AgentEvent event) {
    Object countObj = event.getMetadata().get("escalation_count");
    if (countObj instanceof Integer) {
      return (Integer) countObj;
    }
    return 0;
  }

  @Override
  public void processTimedOutMatch(
      Map<String, List<AgentEvent>> match, Context ctx) throws Exception {

    LOG.warn("Pattern match timed out for tier: {}", tier.getTierName());

    List<AgentEvent> startEvents = match.get("start");
    if (startEvents == null || startEvents.isEmpty()) {
      return;
    }

    AgentEvent startEvent = startEvents.get(0);
    String flowId = startEvent.getFlowId();

    LOG.warn("Tier {} timed out for flow: {}", tier.getTierIndex(), flowId);

    AgentEvent timeoutEvent = startEvent.withEventType(AgentEventType.TIMEOUT_OCCURRED);
    timeoutEvent.incrementIteration();
    timeoutEvent.putMetadata("state", AgentState.FAILED.name());
    timeoutEvent.getData().put("timeout_reason", "Tier execution exceeded time window");
    timeoutEvent.getData().put("tier_index", tier.getTierIndex());
    timeoutEvent.getData().put("tier_name", tier.getTierName());

    ctx.output(TIMEOUT_TAG, timeoutEvent);
  }
}
