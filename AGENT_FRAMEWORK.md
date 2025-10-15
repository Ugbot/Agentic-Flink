# 🏗️ Agent Framework - Complete Guide

**A production-ready agentic framework built on Apache Flink and LangChain4J**

## 📖 Who Should Read This?

- **Beginners:** Start with [CONCEPTS.md](CONCEPTS.md) and [GETTING_STARTED.md](GETTING_STARTED.md) first
- **Building agents:** This guide shows you how everything fits together
- **Production deployment:** Learn about reliability, scaling, and monitoring
- **Advanced users:** Deep dive into CEP patterns, state management, and architecture

## 🎯 What This Framework Does

Think of this framework as a **reliable factory for running AI agents**:

```
Your Input → [Agent Factory] → Reliable Output
             ↓
         - Never loses data
         - Handles failures
         - Scales automatically
         - Validates results
```

### Real-World Analogy

Imagine a **quality-controlled assembly line**:
- Workers (agents) receive tasks
- They use tools to complete tasks
- Quality inspectors (validators) check the work
- If something's wrong, it goes back for correction
- A supervisor steps in for tough cases
- Everything is tracked and reliable

## 🏛️ Architecture Overview

### The Big Picture

```
┌────────────────────────────────────────────────────────────┐
│                    Agent Execution Flow                     │
├────────────────────────────────────────────────────────────┤
│                                                              │
│  Input Events                                                │
│       ↓                                                      │
│  [Tool Execution] ──> [Validation] ──> Success! ✓          │
│       ↓                    ↓                                 │
│    Timeout?            Failed?                               │
│       ↓                    ↓                                 │
│    Retry            [Correction] ──> [Re-validate]          │
│                           ↓                                  │
│                    Still Failed?                             │
│                           ↓                                  │
│                    [Supervisor Review]                       │
│                           ↓                                  │
│                    Manual Decision                           │
│                                                              │
└────────────────────────────────────────────────────────────┘
```

### Core Components (Simplified)

| Component | What It Does | Like... |
|-----------|--------------|---------|
| **AgentEvent** | Messages that agents process | Emails in your inbox |
| **ToolExecutor** | Performs actual work | Apps on your phone |
| **Validator** | Checks if work is correct | Quality inspector |
| **Corrector** | Fixes mistakes | Error correction team |
| **Supervisor** | Reviews tough cases | Manager approval |
| **Context** | Agent's memory | Notebook with notes |

---

## 📁 Project Structure (Explained)

```
com.ververica.flink.agent/
│
├── core/                       ← Building blocks
│   ├── AgentEvent.java         ← The messages agents receive
│   ├── AgentEventType.java     ← Types: STARTED, COMPLETED, FAILED, etc.
│   ├── AgentConfig.java        ← How to configure your agent
│   └── ToolDefinition.java     ← How to define a tool
│
├── serde/                      ← Data models
│   ├── ToolCallRequest.java    ← "Please run this tool"
│   ├── ToolCallResponse.java   ← "Here's what the tool returned"
│   └── ValidationResult.java   ← "Is this correct?"
│
├── function/                   ← Processing logic
│   ├── ToolCallAsyncFunction.java    ← Executes tools asynchronously
│   ├── ValidationFunction.java       ← Checks results
│   ├── CorrectionFunction.java       ← Fixes issues
│   └── SupervisorReviewFunction.java ← Manual review
│
├── pattern/                    ← Workflow patterns (advanced)
│   └── AgentCEPPattern.java    ← Defines event sequences
│
├── stream/                     ← Orchestration
│   └── AgentExecutionStream.java ← Wires everything together
│
├── tools/                      ← Tool implementations
│   └── rag/                    ← Document and search tools
│
├── context/                    ← Memory management
│   ├── core/                   ← Basic context structures
│   ├── memory/                 ← Short & long-term memory
│   ├── compaction/             ← Memory cleanup
│   └── relevancy/              ← Importance scoring
│
├── langchain/                  ← AI model integration
│   ├── client/                 ← Talk to LLMs
│   ├── model/                  ← Different AI models
│   └── store/                  ← Vector database
│
└── example/                    ← Ready-to-run examples
    ├── SimpleAgentExample.java
    ├── RagAgentExample.java
    └── ContextManagementExample.java
```

---

## 🔑 Key Features (With Examples)

### 1. Deterministic Tool Execution

**What it means:** Your tools run reliably, every time.

**Features:**
- ✅ Exactly-once guarantees (never runs twice by accident)
- ✅ Automatic retries if something fails
- ✅ Timeout handling (doesn't hang forever)
- ✅ Error recovery (survives crashes)

**Example:**
```java
// This tool will ALWAYS run exactly once per request
// Even if:
// - The server crashes mid-execution
// - The network fails
// - The database is temporarily down
ToolDefinition emailTool = new ToolDefinition();
emailTool.setToolId("send-email");
emailTool.setMaxRetries(3);         // Try 3 times if it fails
emailTool.setTimeoutMs(30000);      // Give up after 30 seconds
```

**How it works:**
```
Attempt 1: Send email → Network error
  ↓ Wait 1 second (exponential backoff)
Attempt 2: Send email → Database timeout
  ↓ Wait 2 seconds
Attempt 3: Send email → Success! ✓
```

### 2. Validation & Correction

**What it means:** Agents check their work and fix mistakes automatically.

**The Flow:**
```
Agent completes task
  ↓
Validator checks: "Is this correct?"
  ├─ Yes → Done! ✓
  └─ No  → Corrector tries to fix it
              ↓
          Validator checks again
              ├─ Yes → Done! ✓
              └─ No  → Try again (up to max attempts)
                          ↓
                      Send to Supervisor
```

**Example:**
```java
AgentConfig config = new AgentConfig();
config.setValidationEnabled(true);         // Enable checking
config.setMaxCorrectionAttempts(3);        // Try fixing 3 times
config.setSupervisorReviewEnabled(true);   // Ask human if still wrong

// Custom validation logic
config.setValidationPrompt(
    "Check if the result is accurate and complete. " +
    "Look for: 1) Missing information, 2) Incorrect values, " +
    "3) Logic errors. Respond with VALID or INVALID."
);
```

**Real Example - Refund Validation:**
```
Tool: Calculate refund
Result: $99,999.00

Validator: "Wait, the order was only $99.99. This is wrong!"
  ↓ Send to Corrector

Corrector: "Let me recalculate... $99.99"
  ↓ Send back to Validator

Validator: "That's correct!" ✓
```

### 3. Feedback Loops

**What it means:** If something's wrong, try again (smartly).

**Features:**
- Loop back for re-validation after correction
- Max iteration limits (no infinite loops)
- Iteration tracking (know how many attempts)
- Automatic escalation (ask for help if stuck)

**Example:**
```java
AgentConfig config = new AgentConfig();
config.setMaxIterations(5);  // Try up to 5 times total

// What happens:
// Attempt 1: Execute → Validate → Failed → Correct
// Attempt 2: Validate → Failed → Correct
// Attempt 3: Validate → Failed → Correct
// Attempt 4: Validate → Failed → Escalate to Supervisor
```

**Visual:**
```
┌─────────────────────────────────────┐
│   Execute Tool                       │
│         ↓                            │
│   Validate Result                    │
│         ↓                            │
│   Failed? ──Yes──> Correct ──┐      │
│         No                    │      │
│         ↓                     │      │
│   Success! ✓                 │      │
│                               │      │
│   Reached max attempts? ──────┘      │
│         ↓                            │
│   Escalate to Supervisor             │
└─────────────────────────────────────┘
```

### 4. Multiple Output Paths

**What it means:** Events can go different ways based on what happens.

**Think of it like mail sorting:**
```
Incoming Mail (Events)
        ↓
    [Sorter]
        ↓
    ┌───┴───┬────────┬─────────┐
    ↓       ↓        ↓         ↓
Success  Retry   Error   Supervisor
  Box     Box     Box       Box
```

**Code Example:**
```java
// Main output: Successful completions
DataStream<AgentEvent> success = results;

// Side outputs for different scenarios
DataStream<AgentEvent> needRetry = results.getSideOutput(RETRY_TAG);
DataStream<AgentEvent> errors = results.getSideOutput(ERROR_TAG);
DataStream<AgentEvent> needReview = results.getSideOutput(SUPERVISOR_TAG);

// You can handle each separately
success.map(event -> sendSuccessNotification(event));
errors.map(event -> logError(event));
needReview.map(event -> alertHuman(event));
```

### 5. Runtime Configuration Updates

**What it means:** Change agent behavior without restarting.

**Use Cases:**
- Add new tools while system is running
- Update which tools are allowed for specific users
- Change prompts and instructions
- Enable/disable features

**Example:**
```java
// Add a new tool without restarting
ToolDefinition newTool = new ToolDefinition();
newTool.setToolId("new-feature");
newTool.setName("New Feature");

// Broadcast to all running agents
ToolRegistryUpdate update = new ToolRegistryUpdate(newTool);
broadcastStream.send(update);

// All agents now have access to this tool!
```

**Real-World Scenario:**
```
10:00 AM - Agent running with [EmailTool, CalendarTool]
10:30 AM - Product team releases SMSTool
10:31 AM - Broadcast new tool definition
10:32 AM - All agents can now use SMSTool
          (No restart needed!)
```

### 6. State Lifecycle Management

**What it means:** Automatically manage memory and storage efficiently.

**The Problem:**
```
Agent handling 1,000,000 conversations
  ↓
Can't keep all in memory!
  ↓
Need smart storage strategy
```

**The Solution - Multi-Tier Storage:**

```
┌──────────────────────────────────────────┐
│ Tier 1: Flink State (Hot)                │
│ - Active conversations (last 30 min)     │
│ - Super fast access                       │
│ - Limited space                           │
└──────────────────────────────────────────┘
          ↓ Auto-offload after 30 min
┌──────────────────────────────────────────┐
│ Tier 2: Redis Cache (Warm)               │
│ - Recently active (last 24 hours)        │
│ - Fast access                             │
│ - More space                              │
└──────────────────────────────────────────┘
          ↓ Auto-offload after 24 hours
┌──────────────────────────────────────────┐
│ Tier 3: Database (Permanent)             │
│ - Long-term storage                       │
│ - Slower but reliable                     │
│ - Unlimited space                         │
└──────────────────────────────────────────┘
```

**Configuration:**
```java
StateLifecycleConfig lifecycle = new StateLifecycleConfig();

// Automatically move inactive states
lifecycle.setActiveTimeout(Duration.ofMinutes(30));
lifecycle.setWaitingForUserTimeout(Duration.ofHours(24));
lifecycle.setWaitingForToolTimeout(Duration.ofMinutes(10));
lifecycle.setPausedTimeout(Duration.ofDays(7));

// When user returns, automatically restore from right tier
```

**What Happens:**
```
User starts conversation → Tier 1 (Flink state)
  ↓ User goes away for 1 hour
Auto-offload → Tier 2 (Redis)
  ↓ User returns after 2 days
Auto-restore from → Tier 3 (Database)
  ↓ Continue conversation seamlessly!
```

---

## 🚀 Quick Start Guide

### Step 1: Define Your Tools

**Simple Tool Example:**
```java
// Define what your tool does
ToolDefinition calculator = new ToolDefinition(
    "calculator",                         // Unique ID
    "Calculator",                         // Display name
    "Performs arithmetic operations"      // Description
);

// Define inputs
calculator.addInputParameter(
    "operation",    // Parameter name
    "string",       // Data type
    "Operation type (+, -, *, /)",  // Description
    true            // Required?
);
calculator.addInputParameter("a", "number", "First number", true);
calculator.addInputParameter("b", "number", "Second number", true);

// Define output
calculator.setOutputType("number");
calculator.setOutputDescription("Result of the calculation");
```

### Step 2: Configure Your Agent

```java
// Create agent configuration
AgentConfig config = new AgentConfig(
    "my-agent-001",    // Unique agent ID
    "Calculator Agent" // Display name
);

// Add tools this agent can use
config.addAllowedTool("calculator");

// Configure behavior
config.setMaxIterations(5);              // Try up to 5 times
config.setExecutionTimeoutMs(300000L);   // 5 minute timeout

// Enable quality checks
config.setEnableValidation(true);
config.setEnableAutoCorrection(true);
config.setMaxCorrectionAttempts(3);

// LLM configuration
config.setLlmModel("OLLAMA");
config.setSystemPrompt(
    "You are a helpful calculator assistant. " +
    "Always double-check your calculations."
);
```

### Step 3: Create the Execution Stream

**This wires everything together:**

```java
// 1. Setup Flink environment
StreamExecutionEnvironment env =
    StreamExecutionEnvironment.getExecutionEnvironment();
env.setParallelism(4);  // Run 4 agents in parallel

// 2. Create your input events
DataStream<AgentEvent> inputEvents = env.addSource(
    new KafkaSource<>("agent-events-topic")
);

// 3. Create the agent execution stream
AgentExecutionStream agentStream =
    new AgentExecutionStream(env, config, toolRegistry);

// 4. Process events
SingleOutputStreamOperator<AgentEvent> results =
    agentStream.createAgentStream(inputEvents);

// 5. Handle outputs
results.print();  // Print successful results

// Handle side outputs
DataStream<AgentEvent> errors =
    results.getSideOutput(AgentExecutionStream.ERROR_TAG);
errors.addSink(new ErrorLogSink());  // Log errors
```

### Step 4: Run It!

```java
env.execute("My Agent Application");
```

---

## 📊 Event Flow Patterns

### Pattern 1: Happy Path (Everything Works)

```
[Start] → [Tool Requested] → [Tool Executes] → [Validates] → [Complete] ✓

Timeline:
0ms    : FLOW_STARTED (User sends request)
50ms   : TOOL_CALL_REQUESTED (Agent decides to use calculator)
100ms  : TOOL_CALL_COMPLETED (Calculator returns 42)
150ms  : VALIDATION_PASSED (Result is correct)
200ms  : FLOW_COMPLETED (Send result to user)
```

**Code to Generate This Flow:**
```java
AgentEvent event = new AgentEvent();
event.setFlowId("flow-123");
event.setUserId("user-001");
event.setAgentId("calculator-agent");
event.setEventType(AgentEventType.FLOW_STARTED);
event.putData("request", "Calculate 40 + 2");

// Framework handles the rest automatically!
```

### Pattern 2: Correction Path (Oops, Need to Fix)

```
[Start] → [Tool] → [Validates] → [Failed] → [Correct] → [Validates] → [Pass] ✓

Timeline:
0ms    : FLOW_STARTED
50ms   : TOOL_CALL_REQUESTED
100ms  : TOOL_CALL_COMPLETED (Result: 9999)
150ms  : VALIDATION_FAILED (Way too high!)
200ms  : CORRECTION_REQUESTED
250ms  : CORRECTION_COMPLETED (New result: 42)
300ms  : VALIDATION_PASSED (Correct!)
350ms  : FLOW_COMPLETED
```

### Pattern 3: Supervisor Path (Need Human Help)

```
[Start] → [Tool] → [Validate] → [Fail] → [Correct] → [Fail] → [Supervisor]

Timeline:
0ms    : FLOW_STARTED
50ms   : TOOL_CALL_REQUESTED
100ms  : TOOL_CALL_COMPLETED
150ms  : VALIDATION_FAILED (Attempt 1)
200ms  : CORRECTION_COMPLETED
250ms  : VALIDATION_FAILED (Attempt 2)
300ms  : CORRECTION_COMPLETED
350ms  : VALIDATION_FAILED (Attempt 3 - max reached)
400ms  : SUPERVISOR_REVIEW_REQUESTED (Human needed!)
...wait for human...
1200ms : SUPERVISOR_APPROVED (Human says OK)
1250ms : FLOW_COMPLETED
```

---

## ⚙️ Configuration Reference

### AgentConfig - Complete Options

```java
AgentConfig config = new AgentConfig("agent-id", "Agent Name");

// ===== Tool Configuration =====
config.addAllowedTool("tool-id-1");
config.addAllowedTool("tool-id-2");
config.setToolSelectionStrategy("auto");  // or "explicit"

// ===== LLM Configuration =====
config.setLlmModel("OLLAMA");  // or "OPENAI"
config.setModelName("llama2:latest");
config.setSystemPrompt("You are a helpful assistant...");
config.setTemperature(0.7);
config.setMaxTokens(2000);

// ===== Execution Limits =====
config.setMaxIterations(10);              // Max loops
config.setExecutionTimeoutMs(300000L);    // 5 minutes
config.setIdleTimeoutMs(60000L);          // 1 minute of inactivity

// ===== Validation Configuration =====
config.setEnableValidation(true);
config.setValidationPrompt(
    "Check if the result is correct. " +
    "Respond with VALID or INVALID and explain why."
);
config.setValidationModel("OLLAMA");      // Can use different model

// ===== Correction Configuration =====
config.setEnableAutoCorrection(true);
config.setMaxCorrectionAttempts(3);
config.setCorrectionPrompt(
    "The previous result was incorrect. " +
    "Here's the validation feedback: {feedback}. " +
    "Please provide a corrected result."
);

// ===== Supervisor Configuration =====
config.setRequireSupervisor(false);       // Optional human review
config.setSupervisorEscalationThreshold(3); // After 3 failed corrections
config.setSupervisorTimeoutMs(600000L);   // 10 minutes to respond

// ===== Context Management =====
config.setMaxContextTokens(4000);
config.setContextWindowStrategy("sliding"); // or "fixed"
config.setEnableContextCompaction(true);

// ===== State Lifecycle =====
config.setStateTimeoutMs(1800000L);       // 30 minutes
config.setEnableAutoOffload(true);
config.setOffloadToRedis(true);
config.setOffloadToDatabase(true);
```

---

## 🔄 Broadcast State Updates (Advanced)

### What Are Broadcast Updates?

**Analogy:** Like sending a company-wide email that updates everyone's handbook instantly.

```
┌────────────────────────────────────────┐
│  Broadcast Message                      │
│  "New tool available: WeatherAPI"       │
└────────────────────────────────────────┘
           ↓ ↓ ↓ ↓ ↓
    ┌──────┴──────┴──────┴──────┐
    ↓      ↓      ↓      ↓      ↓
 Agent   Agent  Agent Agent  Agent
   1       2      3     4      5

All agents updated simultaneously!
```

### Update Tool Registry

```java
// Create new tool definition
ToolDefinition weatherTool = new ToolDefinition(
    "weather",
    "Weather Service",
    "Gets current weather for a location"
);
weatherTool.addInputParameter("location", "string", "City name", true);

// Broadcast to all running agents
ToolRegistryUpdate update = new ToolRegistryUpdate(
    ToolRegistryAction.ADD,
    weatherTool
);

// Send via broadcast stream
toolRegistryBroadcast.send(update);

// All agents now have access to weather tool!
```

### Update Tool Allowlist (Per User/Agent)

```java
// Allow specific user to use a new tool
ToolAllowlistUpdate update = new ToolAllowlistUpdate(
    "user-123",        // Which user
    "agent-001",       // Which agent
    ToolAllowlistAction.ADD
);
update.addTool("premium-tool");

// Broadcast
allowlistBroadcast.send(update);

// Now user-123 can use premium-tool!
```

### Control Commands

```java
// Clear context for a specific conversation
ControlCommand clearContext = new ControlCommand(
    "user-001:chat-001",              // Target
    ControlCommandType.CLEAR_CONTEXT
);
controlBroadcast.send(clearContext);

// Pause processing
ControlCommand pause = new ControlCommand(
    "user-001:chat-001",
    ControlCommandType.PAUSE
);
controlBroadcast.send(pause);

// Resume
ControlCommand resume = new ControlCommand(
    "user-001:chat-001",
    ControlCommandType.RESUME
);
controlBroadcast.send(resume);
```

---

## 💾 State Management Deep Dive

### Why State Management Matters

**The Problem:**
```
Agent handling conversation with user
  ↓
User asks: "What did I ask 5 minutes ago?"
  ↓
Agent needs to remember!
  ↓
But can't keep everything in memory forever
```

### Automatic Offload Strategy

**Based on Activity State:**

```java
public enum ActivityState {
    ACTIVE,              // Currently processing → Keep in Flink (30 min)
    WAITING_FOR_USER,    // Awaiting user input → Move to Redis (24 hours)
    WAITING_FOR_TOOL,    // Awaiting tool result → Keep in Flink (10 min)
    PAUSED,              // Manually paused → Move to DB (7 days)
    COMPLETED            // Finished → Archive to S3 (forever)
}
```

**Configuration:**
```java
StateLifecycleConfig lifecycle = new StateLifecycleConfig();

// Tier 1: Flink State (hot, fast, limited)
lifecycle.setActiveRetentionTime(Duration.ofMinutes(30));
lifecycle.setWaitingForToolTimeout(Duration.ofMinutes(10));

// Tier 2: Redis (warm, fast, more space)
lifecycle.setWaitingForUserTimeout(Duration.ofHours(24));
lifecycle.enableRedisCaching(true);
lifecycle.setRedisHost("localhost:6379");

// Tier 3: Database (permanent, slower, unlimited)
lifecycle.setPausedTimeout(Duration.ofDays(7));
lifecycle.enableDatabasePersistence(true);

// Tier 4: S3/Data Lake (cold storage, archival)
lifecycle.enableS3Archival(true);
lifecycle.setS3Bucket("agent-state-archive");
```

### Cold Start Restoration

**When a user returns after being away:**

```
User sends new message
  ↓
Check Flink state → Not found
  ↓
Check Redis cache → Not found
  ↓
Check Database → Found! (User was away 3 days)
  ↓
Hydrate state back into Flink
  ↓
Resume conversation seamlessly
  ↓
User doesn't notice anything!
```

**Code:**
```java
public class StateRestorationFunction extends KeyedProcessFunction<...> {

    @Override
    public void processElement(AgentEvent event, Context ctx, ...) {
        String key = event.getFlowId();

        // Try Flink state first (fastest)
        AgentExecutionState state = flinkState.value();

        if (state == null) {
            // Try Redis (fast)
            state = redisCache.get(key);
        }

        if (state == null) {
            // Try Database (slower but permanent)
            state = database.findByFlowId(key);
        }

        if (state == null) {
            // Start fresh
            state = new AgentExecutionState();
        }

        // Continue processing...
    }
}
```

---

## 📈 Production Considerations

### Monitoring

**Key Metrics to Track:**

```java
// Performance Metrics
metrics.gauge("agent.active_flows", () -> countActiveFlows());
metrics.gauge("agent.offloaded_flows", () -> countOffloadedFlows());
metrics.histogram("agent.tool_execution_duration_ms");
metrics.histogram("agent.end_to_end_latency_ms");

// Quality Metrics
metrics.counter("agent.validation_passed");
metrics.counter("agent.validation_failed");
metrics.counter("agent.correction_success");
metrics.counter("agent.supervisor_escalations");

// Reliability Metrics
metrics.counter("agent.tool_timeout");
metrics.counter("agent.tool_retry");
metrics.counter("agent.tool_failure");
metrics.gauge("agent.loop_iterations", () -> currentIterations());

// Resource Metrics
metrics.gauge("agent.memory_usage_bytes");
metrics.gauge("agent.context_size_tokens");
metrics.gauge("agent.state_restoration_latency_ms");
```

**Dashboard Example:**
```
┌─────────────────────────────────────────┐
│  Agent Health Dashboard                  │
├─────────────────────────────────────────┤
│  Active Flows:  1,247                    │
│  Offloaded:    15,832                    │
│  Avg Latency:  234ms                     │
│  Success Rate: 97.3%                     │
│                                          │
│  Validation:                             │
│    ✓ Passed:   95.2%                     │
│    ✗ Failed:    4.8%                     │
│                                          │
│  Corrections:                            │
│    ✓ Fixed:    87.1%                     │
│    → Supervisor: 12.9%                   │
└─────────────────────────────────────────┘
```

### Scaling Strategy

**Horizontal Scaling:**
```java
// Agents are keyed by flowId
DataStream<AgentEvent> keyedStream = events
    .keyBy(event -> event.getFlowId());

// Each key can process independently
// Flink distributes across parallel instances

// Scale by increasing parallelism:
env.setParallelism(16);  // 16 parallel agent instances
```

**Benefits:**
- Each conversation/flow is independent
- No coordination needed between parallel instances
- Linear scalability (2x instances = 2x capacity)
- Automatic load balancing by Flink

**Scaling Example:**
```
1 instance  →  1,000 flows/sec
4 instances →  4,000 flows/sec
8 instances →  8,000 flows/sec
...scale as needed
```

### Error Handling Best Practices

**Retry Strategy:**
```java
ToolExecutionConfig config = new ToolExecutionConfig();
config.setMaxRetries(3);
config.setRetryBackoff(RetryBackoff.EXPONENTIAL);
config.setInitialRetryDelayMs(1000);    // 1 second
config.setMaxRetryDelayMs(30000);       // 30 seconds

// Attempt 1: Immediate
// Attempt 2: After 1 second
// Attempt 3: After 2 seconds
// Attempt 4: After 4 seconds
```

**Dead Letter Queue:**
```java
// Unrecoverable errors go to DLQ
DataStream<AgentEvent> dlq = results.getSideOutput(DLQ_TAG);
dlq.addSink(new KafkaSink<>(
    "agent-errors-dlq",
    new AgentEventSchema()
));

// You can:
// 1. Monitor DLQ for patterns
// 2. Replay after fixing issues
// 3. Alert on DLQ accumulation
```

**Circuit Breaker:**
```java
// Prevent cascading failures
CircuitBreakerConfig cb = new CircuitBreakerConfig();
cb.setFailureThreshold(10);           // Open after 10 failures
cb.setTimeoutDurationMs(60000);       // Reset after 1 minute
cb.setHalfOpenRequests(3);            // Test with 3 requests

// Applied to tool executors
ToolExecutor wrappedExecutor = CircuitBreaker.wrap(
    originalExecutor, cb
);
```

---

## 🔍 Advanced Patterns

### Pattern 1: Multi-Step Workflows

**Example: Customer Onboarding**

```java
// Step 1: Validate customer info
AgentEvent validateEvent = new AgentEvent();
validateEvent.setEventType(AgentEventType.TOOL_CALL_REQUESTED);
validateEvent.putData("toolId", "validate-customer");

// Step 2: Create account (only after Step 1 succeeds)
AgentEvent createEvent = new AgentEvent();
createEvent.setEventType(AgentEventType.TOOL_CALL_REQUESTED);
createEvent.putData("toolId", "create-account");
createEvent.putData("dependsOn", validateEvent.getFlowId());

// Step 3: Send welcome email (only after Step 2)
AgentEvent emailEvent = new AgentEvent();
emailEvent.setEventType(AgentEventType.TOOL_CALL_REQUESTED);
emailEvent.putData("toolId", "send-email");
emailEvent.putData("dependsOn", createEvent.getFlowId());

// Framework ensures correct order and error handling
```

### Pattern 2: Parallel Tool Execution

**Example: Gather Multiple Data Sources**

```java
// Execute 3 tools in parallel
List<String> tools = List.of(
    "check-weather",
    "check-traffic",
    "check-calendar"
);

for (String tool : tools) {
    AgentEvent event = new AgentEvent();
    event.setFlowId("morning-brief-" + UUID.randomUUID());
    event.setEventType(AgentEventType.TOOL_CALL_REQUESTED);
    event.putData("toolId", tool);
    event.putData("parallel", true);

    // All execute simultaneously
    events.send(event);
}

// Wait for all to complete
// Combine results
// Present to user
```

### Pattern 3: Human-in-the-Loop

**Example: Approval Required**

```java
AgentConfig config = new AgentConfig();

// Always require human approval for sensitive actions
config.setRequireSupervisor(true);
config.setSupervisorApprovalRequired(true);

// Define which actions need approval
config.addSensitiveAction("transfer-money");
config.addSensitiveAction("delete-account");
config.addSensitiveAction("change-permissions");

// If tool is sensitive:
if (isSensitive(toolId)) {
    // Pause execution
    // Send notification to human
    // Wait for approval
    // Continue or abort based on decision
}
```

---

## 🛠️ Integration Guide

### With Existing LangChain4J

**The framework reuses your existing LangChain4J setup:**

```java
// Your existing LangChain4J config
LangChainAsyncClient client = new LangChainAsyncClient(
    List.of(
        new OllamaLanguageModel(),
        new OpenAiLanguageModel()
    )
);

// Use it in the framework
AgentExecutionStream stream = new AgentExecutionStream(
    env,
    config,
    toolRegistry,
    client  // ← Your existing client!
);
```

**Supported Models:**
- ✅ Ollama (llama2, codellama, mistral, etc.)
- ✅ OpenAI (gpt-3.5-turbo, gpt-4, etc.)
- ✅ Any LangChain4J-compatible model

### With Apache Kafka

**Event-Driven Architecture:**

```java
// Consume events from Kafka
DataStream<AgentEvent> events = env.addSource(
    KafkaSource.<AgentEvent>builder()
        .setBootstrapServers("localhost:9092")
        .setTopic("agent-events")
        .setValueOnlyDeserializer(new AgentEventDeserializer())
        .build()
);

// Process through agent framework
DataStream<AgentEvent> results = agentStream.createAgentStream(events);

// Publish results back to Kafka
results.sinkTo(
    KafkaSink.<AgentEvent>builder()
        .setBootstrapServers("localhost:9092")
        .setRecordSerializer(new AgentEventSerializer())
        .setProperty("topic", "agent-results")
        .build()
);
```

### With Vector Databases (RAG)

**Qdrant Integration:**

```java
// Configure Qdrant
QdrantConfig qdrantConfig = new QdrantConfig();
qdrantConfig.setHost("localhost");
qdrantConfig.setPort(6333);
qdrantConfig.setCollectionName("agent-knowledge");

// Add RAG tools
ToolExecutorRegistry registry = new ToolExecutorRegistry();
registry.register(new DocumentIngestionTool(qdrantConfig));
registry.register(new SemanticSearchTool(qdrantConfig));
registry.register(new RagQueryTool(qdrantConfig));

// Now agents can search documents!
```

---

## 📚 Additional Resources

### Learning Path

1. **Start Here:** [GETTING_STARTED.md](GETTING_STARTED.md) - Setup and first agent
2. **Understand:** [CONCEPTS.md](CONCEPTS.md) - Core concepts explained simply
3. **Practice:** [EXAMPLES.md](EXAMPLES.md) - Detailed walkthroughs
4. **You Are Here:** AGENT_FRAMEWORK.md - Complete reference
5. **Troubleshoot:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues

### Example Projects

- **Simple Agent:** Basic workflow with validation
- **RAG Agent:** Document processing and search
- **Context Management:** Memory and compaction
- **Weather Agent:** Build your own from scratch

### API Documentation

**Core Classes:**
- `AgentEvent` - Event model
- `AgentConfig` - Configuration
- `ToolDefinition` - Tool schema
- `AgentExecutionStream` - Orchestration

**Tool Classes:**
- `AbstractToolExecutor` - Base for all tools
- `ToolExecutorRegistry` - Tool management
- `ToolCallAsyncFunction` - Async execution

**Context Classes:**
- `AgentContext` - Memory container
- `ContextItem` - Memory items
- `CompactionFunction` - Memory cleanup

---

## 🎯 Next Steps

**For Beginners:**
1. Run the SimpleAgentExample
2. Modify it to use different tools
3. Add your own custom tool
4. Experiment with validation and correction

**For Production:**
1. Set up monitoring and metrics
2. Configure state lifecycle management
3. Implement error handling and retries
4. Test scaling and performance

**For Advanced:**
1. Build multi-step workflows
2. Implement custom CEP patterns
3. Create specialized validation logic
4. Optimize context management

---

## 🤝 Contributing

This framework is built for extensibility. You can:
- Add new tool executors
- Create custom validators
- Implement correction strategies
- Build specialized patterns

---

## 📄 License

Apache License 2.0 - Same as Apache Flink

---

**Ready to build reliable AI agents?** 🚀

Start with [GETTING_STARTED.md](GETTING_STARTED.md) or run an example!
