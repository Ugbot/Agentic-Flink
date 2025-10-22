# Agentic Flink - AI Agents on Apache Flink

> Building AI agents that think like workflows, scale like streams, and reason with context.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]() [![Tests](https://img.shields.io/badge/tests-112%20passing-brightgreen.svg)]() [![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**What if your AI agents could process thousands of requests per second, maintain context across conversations, and orchestrate complex multi-step workflows - all with production-grade reliability?**

That's what we're building with Agentic Flink.

## The Vision

AI agents are powerful, but they need:
- **Orchestration** - coordinating multi-step workflows with validation and error handling
- **Scale** - processing high-throughput event streams in real-time
- **Memory** - managing context windows intelligently without hitting limits
- **Reliability** - production-grade storage, caching, and fault tolerance

This framework combines **Apache Flink's stream processing** with **LangChain4J's LLM integration** to create AI agents that are built for production from day one.

## What Makes This Different

🎯 **Real Working Code** - Not vaporware. 112 tests passing, real LLM integration, actual tool execution.

⚡ **Stream-Native Architecture** - Built on Apache Flink CEP for pattern matching and event-driven orchestration.

🧠 **Smart Context Management** - MoSCoW prioritization keeps critical context, discards noise, and uses RAG for long-term memory.

🏗️ **Production-Ready Storage** - Two-tier architecture (Redis hot cache + PostgreSQL durable storage) with comprehensive test coverage.

🔧 **Extensible Tool Framework** - LangChain4J @Tool annotations with automatic discovery and registration.

📊 **Honest Documentation** - We document what works NOW, not what might work someday.

## Quick Look

```java
// Real working tiered agent with validation → execution → supervision
public class TieredAgentExample {
  public static void main(String[] args) {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    // Create event stream
    DataStream<AgentEvent> events = env.fromElements(
        createCalculationRequest("calc-001", "What is 150 + 275?")
    );

    // Tier 1: Validate requests
    DataStream<AgentEvent> validated = events
        .flatMap(new ValidationAgent())
        .name("Validation Agent");

    // Tier 2: Execute with LLM + tools
    DataStream<AgentEvent> executed = validated
        .flatMap(new ExecutionAgent())
        .name("Execution Agent");

    // Tier 3: Supervisor review
    DataStream<AgentEvent> reviewed = executed
        .flatMap(new SupervisorAgent())
        .name("Supervisor Agent");

    env.execute("Tiered Agent System");
  }
}
```

**This example actually works.** 466 lines of real code, using Ollama for LLM calls and actual tool execution.

## Current Status (v1.0.0-SNAPSHOT)

✅ **BUILD SUCCESS** - Tests: 112 run, 0 failures, 0 errors, 5 skipped

### What's Working Right Now

| Component | Status | Tests | Description |
|-----------|--------|-------|-------------|
| **Context Management** | ✅ Production | 24 | MoSCoW prioritization, 5-phase compaction, temporal relevancy |
| **PostgreSQL Storage** | ✅ Production | 31 | Durable conversation storage with full schema |
| **Redis Caching** | ✅ Production | 5* | Hot tier for active contexts (integration tests) |
| **Tool Framework** | ✅ Working | 18 | LangChain4J integration, @Tool annotations |
| **Flink CEP** | ✅ Working | - | Pattern matching, tiered orchestration |
| **Tiered Agents** | ✅ Working | - | Real 3-tier example with LLM + tools |
| **Storage Factory** | ✅ Production | 18 | Multi-backend storage abstraction |
| **Docker Setup** | ✅ Working | - | PostgreSQL + Redis + Ollama |

*Integration tests require running Redis instance (disabled by default)

### Example Applications (14 Working Examples)

- `TieredAgentExample` - Validation → Execution → Supervision workflow
- `ToolAnnotationExample` - LangChain4J @Tool integration
- `RagAgentExample` - Document ingestion and semantic search
- `StorageIntegratedFlinkJob` - PostgreSQL + Redis integration
- `ContextManagementExample` - MoSCoW prioritization demo
- ...and 9 more in `src/main/java/com/ververica/flink/agent/example/`

### In Active Development

🚧 Advanced CEP patterns for multi-agent coordination
🚧 Performance benchmarking and optimization
🚧 Vector search with Qdrant (RAG v2)
🚧 Monitoring and observability

See [STATUS.md](STATUS.md) for detailed component breakdown and [ROADMAP.md](ROADMAP.md) for future plans.

## Architecture: How It Works

```
┌─────────────────────────────────────────────────────────────┐
│                  Agentic Flink Architecture                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Event Stream (mock/Kafka)                                  │
│       ↓                                                       │
│  ┌──────────────────────────────────────────────┐           │
│  │  Flink CEP Pattern Matching                  │           │
│  │  • Validation patterns                       │           │
│  │  • Execution triggers                        │           │
│  │  • Escalation detection                      │           │
│  └──────────────────┬───────────────────────────┘           │
│                     ↓                                         │
│  ┌──────────────────────────────────────────────┐           │
│  │  LangChain4J Integration                     │           │
│  │  • LLM calls (OpenAI/Ollama)                 │           │
│  │  • Tool execution                            │           │
│  │  • Prompt management                         │           │
│  └──────────────────┬───────────────────────────┘           │
│                     ↓                                         │
│  ┌──────────────────────────────────────────────┐           │
│  │  Context Management (MoSCoW)                 │           │
│  │  • Priority-based compaction                 │           │
│  │  • Relevancy scoring                         │           │
│  │  • Memory management                         │           │
│  └──────────────────┬───────────────────────────┘           │
│                     ↓                                         │
│  ┌──────────────────────────────────────────────┐           │
│  │  Two-Tier Storage                            │           │
│  │  • Redis (HOT) - Active contexts             │           │
│  │  • PostgreSQL (WARM) - Durable storage       │           │
│  └──────────────────────────────────────────────┘           │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### The Flow

1. **Events arrive** → Agent requests come in as Flink events (eventually from Kafka, currently mock sources)
2. **CEP patterns match** → Flink CEP detects validation triggers, execution requests, escalation conditions
3. **LLM decides** → LangChain4J calls Ollama/OpenAI to decide which tools to use
4. **Tools execute** → Registered tools perform actual work (calculations, API calls, etc.)
5. **Context manages** → MoSCoW prioritization keeps important context, discards noise
6. **Storage persists** → Redis caches hot data, PostgreSQL stores durable history

All of this happens in a streaming, scalable, fault-tolerant way thanks to Apache Flink.

## 🚀 Get Started in 5 Minutes

### Prerequisites

- **Java 11+** ([Download](https://adoptium.net/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Docker & Docker Compose** (For infrastructure)

### Try It Out

```bash
# 1. Clone the repo
git clone <your-repo-url>
cd Agentic-Flink

# 2. Start infrastructure (PostgreSQL + Redis + Ollama)
docker compose up -d

# 3. Pull the LLM model (small and fast)
docker compose exec ollama ollama pull qwen2.5:3b

# 4. Build the project
mvn clean compile

# 5. Run the tests (112 tests - should all pass!)
mvn test

# 6. Run a real working example
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.TieredAgentExample"
```

**What you'll see:**
- Real Ollama LLM making decisions
- Actual tool execution (calculator tools)
- Three-tier validation → execution → supervision flow
- Context management in action

### Explore the Examples

We have **14 working examples** to learn from:

```bash
# Context management with MoSCoW prioritization
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.ContextManagementExample"

# RAG with document ingestion and search
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.RagAgentExample"

# Storage integration (PostgreSQL + Redis)
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.StorageIntegratedFlinkJob"

# See src/main/java/com/ververica/flink/agent/example/ for all 14 examples
```

## 📚 Documentation & Learning

**Start Here:**
- **[CONCEPTS.md](CONCEPTS.md)** - Core concepts explained simply (agents, tools, context, RAG)
- **[GETTING_STARTED.md](GETTING_STARTED.md)** - Detailed setup guide
- **[CONTEXT_MANAGEMENT.md](CONTEXT_MANAGEMENT.md)** - MoSCoW prioritization and compaction algorithms

**Deep Dives:**
- **[STORAGE_ARCHITECTURE.md](STORAGE_ARCHITECTURE.md)** - Two-tier storage design (Redis + PostgreSQL)
- **[STORAGE_QUICKSTART.md](STORAGE_QUICKSTART.md)** - Storage setup and integration
- **[AGENT_FRAMEWORK.md](AGENT_FRAMEWORK.md)** - Agent patterns and workflows

**Project Status:**
- **[STATUS.md](STATUS.md)** - Detailed component status (what's done, what's in progress)
- **[ROADMAP.md](ROADMAP.md)** - Future plans and version timeline
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - Comprehensive overview

**Examples in Code:**
- Browse `src/main/java/com/ververica/flink/agent/example/` for 14 working examples
- Each example is documented and runnable

## 🔑 Core Concepts (Quick Version)

**Agents** = Programs that reason, use tools, and learn from feedback
```
Task → LLM Decides → Execute Tools → Validate → Done (or retry)
```

**Tools** = Capabilities agents can use (search, calculate, API calls, etc.)

**Context Management** = MoSCoW prioritization keeps critical info, discards noise
- MUST: Critical (always keep)
- SHOULD: Important (usually keep)
- COULD: Nice-to-have (maybe keep)
- WON'T: Temporary (discard)

**RAG** = Retrieval Augmented Generation - connecting agents to your data
```
Documents → Chunk → Embed → Vector Store → Semantic Search
```

**Tiered Agents** = Multi-stage workflows with validation and supervision
```
Validation → Execution → Review → (Escalate if needed)
```

👉 See [CONCEPTS.md](CONCEPTS.md) for detailed explanations

## 🎯 Why Build This?

**The Problem:**
AI agents are great at reasoning, but terrible at scale. LangChain and LangGraph run on single machines. When you need to process thousands of agent requests per second, maintain context across sessions, and orchestrate complex workflows with reliability - you need something more.

**The Solution:**
Combine the orchestration power of Apache Flink with the LLM capabilities of LangChain4J. Get event-driven agent workflows that scale horizontally, persist state reliably, and handle failures gracefully.

**Real-World Use Cases:**
- 🎫 **Customer Support** - Tiered validation → execution → escalation for support tickets
- 📊 **Data Analysis** - Stream events through RAG-enabled agents for real-time insights
- 🔄 **Workflow Automation** - Complex multi-step processes with LLM decision-making
- 🏦 **Financial Processing** - Saga patterns with validation and compensation
- 🤖 **AI Pipelines** - Production-grade agent orchestration at scale

**What Makes Flink Special:**
- ✅ Process millions of events/sec
- ✅ Exactly-once state consistency
- ✅ Automatic checkpointing and recovery
- ✅ CEP for pattern matching workflows
- ✅ Horizontal scaling built-in

## 🤝 Contributing & Community

This started as a learning project, but it's becoming real. Contributions welcome!

**How to Help:**
- 🐛 Report bugs or issues
- 💡 Suggest features or improvements
- 📝 Improve documentation
- 🧪 Share your experiments and use cases
- 🔧 Submit PRs (especially for additional storage backends!)
- ⭐ Star the repo if you find it useful

**Discussion:**
- Open issues for questions and ideas
- Share what you're building with Agentic Flink
- Help others getting started

## 📝 License

Apache License 2.0 - Free to use, modify, and distribute. See [LICENSE](LICENSE) for details.

## 🙏 Built With

Standing on the shoulders of giants:

- **[Apache Flink](https://flink.apache.org/)** - The streaming engine that makes it all possible
- **[LangChain4J](https://github.com/langchain4j/langchain4j)** - Java-native LLM integration
- **[Ollama](https://ollama.ai/)** - Local LLM inference made easy
- **PostgreSQL & Redis** - Battle-tested storage that just works

Special thanks to the Apache Flink Agents team for pioneering agent orchestration on Flink.

## 👤 Author

**Ben Gamble**

Building AI agents that scale. Learning stream processing. Sharing the journey.

- Exploring the intersection of AI agents and stream processing
- Focused on production-ready implementations
- Committed to honest documentation

## ⚠️ Project Status & Philosophy

**What This Project Is:**
- ✅ Real working code (112 passing tests)
- ✅ Production-quality implementations
- ✅ Honest documentation
- ✅ Active development with regular updates

**What This Project Is NOT:**
- ❌ Vaporware with fake demos
- ❌ Production-tested at massive scale (yet)
- ❌ A complete enterprise solution (yet)
- ❌ Overpromising future features

**Our Philosophy:**
1. **Build it before claiming it** - No fake demos, only real working code
2. **Test everything** - If it's not tested, it's not done
3. **Document honestly** - What works now vs. what's planned
4. **Production-focused** - Building for real use cases, not toy examples
5. **Community-driven** - Open to feedback and contributions

**Security Note:**
- 🔐 Never commit API keys or secrets to git
- 🔐 Use environment variables (see `.env.example`)
- 🔐 Review code before deploying anywhere
- 🔐 This is experimental software - use appropriate caution

---

## 🎉 Ready to Build?

```bash
# Get started now
git clone <your-repo-url>
cd Agentic-Flink
docker compose up -d
mvn clean compile
mvn test

# Run a real example
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.TieredAgentExample"
```

**Questions? Issues? Ideas?**

Open a GitHub issue - we'd love to hear from you!

---

**Building the future of scalable AI agents, one passing test at a time.** 🚀
