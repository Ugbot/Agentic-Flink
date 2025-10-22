package com.ververica.flink.agent.dsl;

import com.ververica.flink.agent.completion.TaskList;
import com.ververica.flink.agent.context.manager.ContextWindowManager;
import com.ververica.flink.agent.statemachine.AgentStateMachine;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

/**
 * Immutable agent definition created via the declarative builder API.
 *
 * <p>An Agent represents a complete specification of an AI agent's behavior, including:
 * <ul>
 *   <li>LLM configuration (model, prompt, temperature)</li>
 *   <li>Tool access (which tools this agent can call)</li>
 *   <li>Validation & correction settings</li>
 *   <li>Supervisor chain integration</li>
 *   <li>Context management configuration</li>
 *   <li>State machine behavior</li>
 *   <li>Completion tracking (task lists)</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * Agent researchAgent = Agent.builder()
 *     .withId("research-agent")
 *     .withName("Research Specialist")
 *     .withSystemPrompt("You are a research specialist. Gather and synthesize information.")
 *     .withTools("web-search", "document-analysis", "synthesis")
 *     .withLlmModel("qwen2.5:7b")
 *     .withTemperature(0.3)
 *     .withMaxIterations(10)
 *     .withTimeout(Duration.ofMinutes(5))
 *     .withValidationEnabled(true)
 *     .withSupervisor("quality-supervisor")
 *     .withMaxTokens(8000)
 *     .build();
 * }</pre>
 *
 * <p>The Agent is immutable after creation and can be safely shared across Flink task managers.
 *
 * @author Agentic Flink Team
 * @see AgentBuilder
 */
public class Agent implements Serializable {

  private static final long serialVersionUID = 1L;

  // ==================== Core Identity ====================

  private final String agentId;
  private final String agentName;
  private final String description;
  private final AgentType agentType;

  // ==================== LLM Configuration ====================

  private final String systemPrompt;
  private final String llmModel;
  private final double temperature;
  private final int maxTokens;
  private final int maxResponseTokens;

  // ==================== Tool Configuration ====================

  private final Set<String> allowedTools;
  private final Set<String> requiredTools;
  private final Map<String, Object> toolDefaults;

  // ==================== Execution Configuration ====================

  private final int maxIterations;
  private final Duration timeout;
  private final Duration toolTimeout;
  private final int maxRetries;

  // ==================== Validation & Correction ====================

  private final boolean validationEnabled;
  private final int maxValidationAttempts;
  private final boolean correctionEnabled;
  private final int maxCorrectionAttempts;
  private final String validationPrompt;
  private final String correctionPrompt;

  // ==================== Supervision ====================

  private final String supervisorId;
  private final boolean supervisorReviewRequired;
  private final double supervisorThreshold;

  // ==================== Context Management ====================

  private final ContextWindowManager.ContextWindowConfig contextConfig;
  private final boolean contextCompressionEnabled;

  // ==================== State Machine ====================

  private final AgentStateMachine stateMachine;

  // ==================== Completion Tracking ====================

  private final TaskList taskList;
  private final boolean autoDetectTaskCompletion;

  // ==================== Saga Integration ====================

  private final boolean compensationEnabled;
  private final Map<String, Object> compensationConfig;

  // Package-private constructor - use builder
  Agent(AgentBuilder builder) {
    // Core identity
    this.agentId = builder.agentId;
    this.agentName = builder.agentName;
    this.description = builder.description;
    this.agentType = builder.agentType;

    // LLM configuration
    this.systemPrompt = builder.systemPrompt;
    this.llmModel = builder.llmModel;
    this.temperature = builder.temperature;
    this.maxTokens = builder.maxTokens;
    this.maxResponseTokens = builder.maxResponseTokens;

    // Tool configuration
    this.allowedTools = Collections.unmodifiableSet(new HashSet<>(builder.allowedTools));
    this.requiredTools = Collections.unmodifiableSet(new HashSet<>(builder.requiredTools));
    this.toolDefaults = Collections.unmodifiableMap(new HashMap<>(builder.toolDefaults));

    // Execution configuration
    this.maxIterations = builder.maxIterations;
    this.timeout = builder.timeout;
    this.toolTimeout = builder.toolTimeout;
    this.maxRetries = builder.maxRetries;

    // Validation & correction
    this.validationEnabled = builder.validationEnabled;
    this.maxValidationAttempts = builder.maxValidationAttempts;
    this.correctionEnabled = builder.correctionEnabled;
    this.maxCorrectionAttempts = builder.maxCorrectionAttempts;
    this.validationPrompt = builder.validationPrompt;
    this.correctionPrompt = builder.correctionPrompt;

    // Supervision
    this.supervisorId = builder.supervisorId;
    this.supervisorReviewRequired = builder.supervisorReviewRequired;
    this.supervisorThreshold = builder.supervisorThreshold;

    // Context management
    this.contextConfig = builder.contextConfig;
    this.contextCompressionEnabled = builder.contextCompressionEnabled;

    // State machine
    this.stateMachine = builder.stateMachine;

    // Completion tracking
    this.taskList = builder.taskList;
    this.autoDetectTaskCompletion = builder.autoDetectTaskCompletion;

    // Saga integration
    this.compensationEnabled = builder.compensationEnabled;
    this.compensationConfig = Collections.unmodifiableMap(new HashMap<>(builder.compensationConfig));
  }

  // ==================== Getters ====================

  public String getAgentId() { return agentId; }
  public String getAgentName() { return agentName; }
  public String getDescription() { return description; }
  public AgentType getAgentType() { return agentType; }

  public String getSystemPrompt() { return systemPrompt; }
  public String getLlmModel() { return llmModel; }
  public double getTemperature() { return temperature; }
  public int getMaxTokens() { return maxTokens; }
  public int getMaxResponseTokens() { return maxResponseTokens; }

  public Set<String> getAllowedTools() { return allowedTools; }
  public Set<String> getRequiredTools() { return requiredTools; }
  public Map<String, Object> getToolDefaults() { return toolDefaults; }

  public int getMaxIterations() { return maxIterations; }
  public Duration getTimeout() { return timeout; }
  public Duration getToolTimeout() { return toolTimeout; }
  public int getMaxRetries() { return maxRetries; }

  public boolean isValidationEnabled() { return validationEnabled; }
  public int getMaxValidationAttempts() { return maxValidationAttempts; }
  public boolean isCorrectionEnabled() { return correctionEnabled; }
  public int getMaxCorrectionAttempts() { return maxCorrectionAttempts; }
  public String getValidationPrompt() { return validationPrompt; }
  public String getCorrectionPrompt() { return correctionPrompt; }

  public String getSupervisorId() { return supervisorId; }
  public boolean isSupervisorReviewRequired() { return supervisorReviewRequired; }
  public double getSupervisorThreshold() { return supervisorThreshold; }

  public ContextWindowManager.ContextWindowConfig getContextConfig() { return contextConfig; }
  public boolean isContextCompressionEnabled() { return contextCompressionEnabled; }

  public AgentStateMachine getStateMachine() { return stateMachine; }

  public TaskList getTaskList() { return taskList; }
  public boolean isAutoDetectTaskCompletion() { return autoDetectTaskCompletion; }

  public boolean isCompensationEnabled() { return compensationEnabled; }
  public Map<String, Object> getCompensationConfig() { return compensationConfig; }

  // ==================== Helper Methods ====================

  /**
   * Checks if this agent is allowed to use a specific tool.
   *
   * @param toolName The tool name to check
   * @return true if the tool is in the allowed list
   */
  public boolean canUseTool(String toolName) {
    return allowedTools.contains(toolName);
  }

  /**
   * Checks if this agent has a supervisor configured.
   *
   * @return true if supervisorId is set
   */
  public boolean hasSupervisor() {
    return supervisorId != null && !supervisorId.isEmpty();
  }

  /**
   * Checks if this agent requires completion tracking.
   *
   * @return true if taskList is configured
   */
  public boolean requiresCompletionTracking() {
    return taskList != null;
  }

  /**
   * Creates a builder initialized with this agent's configuration.
   *
   * <p>Useful for creating modified copies of agents.
   *
   * @return builder pre-populated with this agent's settings
   */
  public AgentBuilder toBuilder() {
    return new AgentBuilder()
        .withId(this.agentId)
        .withName(this.agentName)
        .withDescription(this.description)
        .withType(this.agentType)
        .withSystemPrompt(this.systemPrompt)
        .withLlmModel(this.llmModel)
        .withTemperature(this.temperature)
        .withMaxTokens(this.maxTokens)
        .withMaxResponseTokens(this.maxResponseTokens)
        .withTools(this.allowedTools.toArray(new String[0]))
        .withMaxIterations(this.maxIterations)
        .withTimeout(this.timeout)
        .withValidationEnabled(this.validationEnabled)
        .withCorrectionEnabled(this.correctionEnabled)
        .withSupervisor(this.supervisorId)
        .withCompensationEnabled(this.compensationEnabled);
  }

  /**
   * Creates a new builder for defining an agent.
   *
   * @return new agent builder
   */
  public static AgentBuilder builder() {
    return new AgentBuilder();
  }

  @Override
  public String toString() {
    return String.format(
        "Agent[id=%s, name=%s, type=%s, model=%s, tools=%d, supervisor=%s]",
        agentId, agentName, agentType, llmModel, allowedTools.size(),
        hasSupervisor() ? supervisorId : "none");
  }

  // ==================== Agent Types ====================

  /**
   * Enum defining different types of agents.
   *
   * <p>This allows pre-configured agent templates and routing logic.
   */
  public enum AgentType {
    /**
     * Simple executor agent - performs tasks without validation or supervision.
     */
    EXECUTOR("Executor", "Performs tasks with tool calling"),

    /**
     * Validator agent - validates inputs or outputs.
     */
    VALIDATOR("Validator", "Validates data against rules or schemas"),

    /**
     * Corrector agent - attempts to fix validation failures.
     */
    CORRECTOR("Corrector", "Fixes validation failures using LLM feedback"),

    /**
     * Supervisor agent - reviews and approves work from other agents.
     */
    SUPERVISOR("Supervisor", "Reviews and approves agent outputs"),

    /**
     * Coordinator agent - orchestrates multiple sub-agents.
     */
    COORDINATOR("Coordinator", "Orchestrates multiple sub-agent workflows"),

    /**
     * Research agent - specializes in information gathering and synthesis.
     */
    RESEARCHER("Researcher", "Gathers and synthesizes information"),

    /**
     * Custom agent - user-defined behavior.
     */
    CUSTOM("Custom", "User-defined agent behavior");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }
}
