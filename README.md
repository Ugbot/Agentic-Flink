# Agentic Flink - AI Agents on Apache Flink

**Status:** Experimental framework for building AI agents on Apache Flink
**Author:** Ben Gamble
**License:** Apache License 2.0

This is a research project exploring integration of AI agent patterns with Apache Flink's distributed stream processing capabilities.

## Overview

Experimental framework for building AI agents with stream processing capabilities:

- AI decision making (OpenAI/Ollama integration)
- Tool execution framework
- Context management with MoSCoW prioritization
- Validation and correction patterns
- Error handling and retry logic
- Built on Apache Flink for distributed processing

Potential applications include customer service automation, intelligent data pipelines, and adaptive workflows. This is research-grade software exploring agent patterns in streaming systems.

## Key Components

| Component | Description | Status |
|-----------|-------------|--------|
| **Flink Integration** | Leverages Flink's state management and fault tolerance | Partial |
| **Flink Agents Integration** | Adapters for Apache Flink Agents v0.2-SNAPSHOT | Template |
| **Context Management** | MoSCoW prioritization and intelligent compaction | Algorithm complete, state integration partial |
| **Pluggable Storage** | Multi-tier storage architecture (in-memory, Redis, PostgreSQL) | ✅ **Production-ready** |
| **RAG Tools** | Document ingestion and semantic search | Template implementations |
| **Tool Framework** | Extensible tool execution system | Working |

### Apache Flink Agents Integration

Experimental integration with Apache Flink Agents (v0.2-SNAPSHOT). Requires building Flink Agents from source.

**Components:**
- Event adapters (bidirectional conversion)
- Tool adapters (wrapping for Flink Agents actions)
- ReAct agent patterns
- MCP protocol support

**Status:** Template implementations provided. Not production-tested. Requires Flink Agents built from source.

## 🎮 Try the Interactive Demo

**Explore the Flink Agents integration with an interactive demo:**

```bash
./run-demo.sh
```

**What you'll see:**
- Real-time event conversion between frameworks
- Tool execution via Flink Agents
- Validation and context management in action
- Full customer support workflow example
- Performance testing
- Architecture visualization

**Demo includes:**
1. Order Lookup Tool Demo
2. Refund Processing Demo (multi-attempt validation)
3. Knowledge Base Search Demo (MoSCoW context management)
4. Full Customer Support Workflow (end-to-end)
5. System Status Monitoring
6. Performance Test (100 events)
7. Architecture Diagram

See [DEMO_GUIDE.md](DEMO_GUIDE.md) for details.

## 🚀 Quick Start

### Prerequisites

You'll need:
- **Java 11 or higher** - [Download here](https://adoptium.net/)
- **Maven 3.6+** - [Download here](https://maven.apache.org/download.cgi)
- **Ollama** (optional for local AI) - [Download here](https://ollama.ai)

### Basic Setup

```bash
# Clone the repo
git clone <your-repo-url>
cd Agentic-Flink

# Checkout the flink-agents branch for the integration
git checkout flink-agents

# Build
mvn clean compile

# Run the interactive demo
./run-demo.sh
```

### For OpenAI Integration

```bash
# Set your API key
export OPENAI_API_KEY="sk-your-key-here"

# Run OpenAI demo
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.OpenAIFlinkAgentsDemo"
```

See [OPENAI_SETUP.md](OPENAI_SETUP.md) for complete setup.

## 📚 Documentation

**Getting Started:**
- [DEMO_GUIDE.md](DEMO_GUIDE.md) - Interactive demo walkthrough
- [DEMO_QUICK_REF.md](DEMO_QUICK_REF.md) - Quick reference card
- [OPENAI_SETUP.md](OPENAI_SETUP.md) - OpenAI integration guide
- [STORAGE_QUICKSTART.md](STORAGE_QUICKSTART.md) - **Pluggable storage quick start** 🔥

**Integration Details:**
- [FLINK_AGENTS_INTEGRATION.md](FLINK_AGENTS_INTEGRATION.md) - Flink Agents integration architecture
- [INTEGRATION_SUCCESS.md](INTEGRATION_SUCCESS.md) - Integration implementation report

**Storage Architecture:**
- [SESSION_DELIVERY.md](SESSION_DELIVERY.md) - Storage implementation details
- [TEST_SUITE_DELIVERY.md](TEST_SUITE_DELIVERY.md) - Test coverage report
- [COMPLETE_SESSION_SUMMARY.md](COMPLETE_SESSION_SUMMARY.md) - Complete delivery summary

**Examples:**
- `FlinkAgentsIntegrationExample.java` - Static integration examples
- `InteractiveFlinkAgentsDemo.java` - Interactive demo scenarios
- `OpenAIFlinkAgentsDemo.java` - OpenAI integration examples
- `StorageIntegratedFlinkJob.java` - **Storage backend switching demo** 🔥

## 🔑 Core Concepts

### What's an Agent?

An **agent** is a program that:
1. Gets a task (via events)
2. Decides what tools to use
3. Uses those tools
4. Checks if the work is good
5. Either finishes or tries again

```
Task → Think → Use Tools → Check Work → Done!
         ↑                      |
         └──── Try Again ───────┘
```

### What Are Tools?

**Tools** are capabilities your agent can use:
- 🔍 Search a database
- 📧 Send an email
- 🧮 Calculate something
- 📄 Read a document
- 🌐 Call an API

### Context Management

**Context management** helps agents remember what's important:
- **MUST:** Critical information (always kept)
- **SHOULD:** Important data (usually kept)
- **COULD:** Nice-to-have (maybe kept)
- **WON'T:** Temporary (discarded)

### RAG (Retrieval Augmented Generation)

**RAG** connects your AI to your own data:
```
Your Documents → Chunk → Embed → Store → Search when needed
```

## 🎯 Project Status

### ✅ What Works

- **Basic agent workflows** - Event-driven agents with tools
- **Flink Agents integration** - Adapters work, can convert events and wrap tools
- **Demo scenarios** - 7 interactive demos showing capabilities
- **OpenAI integration** - Connect to OpenAI models
- **Tool execution** - Extensible tool framework
- **Event conversion** - Bidirectional, lossless conversion proven
- **Performance** - Fast event processing (16,000+ events/sec in tests)

### ⚠️ Experimental / In Progress

- **Production hardening** - Not battle-tested in production
- **Error recovery** - Basic error handling, needs more robustness
- **Documentation** - Good starting point, could be more comprehensive
- **Test coverage** - Some tests exist, more needed
- **Deployment guides** - Not yet documented
- **Scalability testing** - Small-scale testing only

### 🔮 Future Ideas

- Contribute patterns back to Apache Flink Agents
- More comprehensive testing
- Production deployment patterns
- More tool integrations
- Better observability
- Community feedback and iteration

## 🤝 Contributing

This is a personal learning project, but feedback and ideas are welcome!

- Open issues for bugs or suggestions
- Share your experiments
- Suggest improvements
- Help with documentation

## 📝 License

Apache License 2.0 - Use freely, but no warranties provided.

## 👤 Author

**Ben Gamble**
- Exploring AI agents + stream processing
- Experimenting with Apache Flink patterns
- Learning in public

## 🙏 Acknowledgments

- Apache Flink community for the amazing streaming framework
- Apache Flink Agents team for the official agent framework
- LangChain4J for LLM integration
- Everyone sharing knowledge about AI agents

## ⚠️ Important Notes

**This is experimental software:**
- Not tested in production environments
- APIs may change
- Use at your own risk
- Great for learning and experimentation
- Not recommended for critical systems yet

**Security:**
- Never commit API keys to git
- Use environment variables for secrets
- Review code before using in any production-like environment

## 🚀 Get Started

```bash
# Try the demo
./run-demo.sh

# Read the guides
cat DEMO_GUIDE.md

# Explore the code
ls src/main/java/com/ververica/flink/agent/

# Experiment and learn!
```

---

**Built with curiosity and Apache Flink** 🚀

Questions? Open an issue or reach out!
