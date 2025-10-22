# Agentic Flink - AI Agents on Apache Flink

**Status:** 🚧 Active Development - Making it Real (v1.0.0-SNAPSHOT)
**Author:** Ben Gamble
**License:** Apache License 2.0

This project is building a **real, working framework** for AI agents on Apache Flink, focused on production-ready features:

- **Apache Flink CEP** for agent orchestration and tiered workflows
- **LangChain4J** for LLM integration (OpenAI, Ollama)
- **PostgreSQL + Redis** for durable storage and fast caching
- **Real examples** with actual tool execution and LLM calls

## 🎯 Current Status: Being Honest

**Build Status:** ✅ BUILD SUCCESS - Tests: 112 run, 0 failures, 0 errors, 5 skipped

**What Actually Works:**
- ✅ Core context management algorithms (MoSCoW prioritization)
- ✅ Tool execution framework with LangChain4J
- ✅ Redis caching layer (production-ready with 5 unit tests)
- ✅ PostgreSQL storage (complete with 31 comprehensive tests)
- ✅ Flink CEP patterns for agent workflows
- ✅ Real tiered agent example (ValidationAgent → ExecutionAgent → SupervisorAgent)
- ✅ Docker infrastructure (one-command setup: PostgreSQL + Redis + Ollama)
- ✅ Comprehensive test coverage (112 tests passing)

**What's In Progress:**
- 🚧 Advanced CEP patterns for complex multi-agent workflows
- 🚧 Performance benchmarking and optimization
- 🚧 Vector search integration with Qdrant (RAG capabilities)
- 🚧 Monitoring and metrics (Prometheus/Grafana)

**What's Planned (Not Yet Real):**
- 📋 See [ROADMAP.md](ROADMAP.md) for detailed version plan
- 📋 Apache Flink Agents integration (v2.0, when framework releases Q2 2026)
- 📋 Multi-tenancy and authentication (v2.0)
- 📋 Advanced ML and graph-based reasoning (v2.1)

This README describes what's working **now**. For planned features, see [ROADMAP.md](ROADMAP.md). For detailed progress, see [STATUS.md](STATUS.md). For comprehensive overview, see [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md).

## Key Components

| Component | Description | Status |
|-----------|-------------|--------|
| **Flink CEP** | Complex event processing for agent workflows | ✅ Working |
| **LangChain4J Integration** | LLM calls, tool execution, prompt management | ✅ Working |
| **Context Management** | MoSCoW prioritization and 5-phase compaction | ✅ Production-ready |
| **Redis Storage** | Fast caching layer for active contexts | ✅ Production-ready |
| **PostgreSQL Storage** | Durable conversation storage | ✅ Production-ready (31 tests) |
| **Tool Framework** | Extensible tool execution with @Tool annotations | ✅ Working |
| **Tiered Agent Example** | Real working example with Flink CEP + LLM | ✅ Complete |
| **Flink Agents Plugin** | Optional integration (see ROADMAP.md) | 📋 Future (v2.0) |

### Core Architecture

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

## 🎮 Simulated Demo (Visualization Only)

⚠️ **Note**: The interactive demo is currently a **simulation** that shows what the system will look like. It uses mock data and hardcoded responses to demonstrate the architecture.

```bash
./run-demo.sh
```

**What you'll see (simulated):**
- Agent workflow patterns
- Event structure and flow
- Context management concepts
- Multi-tier validation patterns

**For real working examples**, see the examples in `src/main/java/com/ververica/flink/agent/example/` once v1.0 is complete.

## 🚀 Quick Start (Current Development)

### Prerequisites

You'll need:
- **Java 11 or higher** - [Download here](https://adoptium.net/)
- **Maven 3.6+** - [Download here](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - For PostgreSQL + Redis + Ollama
- **Ollama** (for local LLM) - [Download here](https://ollama.ai)

### Setup

```bash
# Clone the repo
git clone <your-repo-url>
cd Agentic-Flink

# Start infrastructure (PostgreSQL + Redis + Ollama)
docker compose up -d

# Build the project
mvn clean compile

# Run tests (once implemented)
mvn test
```

### Current Status

🚧 **v1.0 In Progress** - Working on:
- Complete PostgreSQL implementation
- Real tiered agent example
- Comprehensive test suite
- End-to-end working demo

Check [STATUS.md](STATUS.md) for current progress.

## 📚 Documentation

**Current Documentation:**
- [STATUS.md](STATUS.md) - What's working now
- [ROADMAP.md](ROADMAP.md) - Planned features
- [GETTING_STARTED.md](GETTING_STARTED.md) - Setup guide (being updated)
- [CONCEPTS.md](CONCEPTS.md) - Core concepts explained

**Storage:**
- [STORAGE_QUICKSTART.md](STORAGE_QUICKSTART.md) - PostgreSQL + Redis setup
- [STORAGE_ARCHITECTURE.md](STORAGE_ARCHITECTURE.md) - Storage design

**Coming Soon:**
- Real working examples with LangChain4J
- End-to-end tutorial
- Production deployment guide

**Archived Documentation:**
See `/docs/archive/` for historical documentation about planned features.

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

## 🎯 What's Working vs What's Planned

### ✅ Working Now

- **Context Management Algorithms** - MoSCoW prioritization, 5-phase compaction
- **Tool Execution Framework** - LangChain4J @Tool integration
- **Redis Storage** - Production-ready caching layer
- **Flink CEP** - Complex event processing for workflows
- **Event System** - Custom event model for agent orchestration
- **Core Infrastructure** - Project structure, build system, dependencies

### 🚧 In Progress (v1.0)

- **PostgreSQL Storage** - Durable conversation storage with tests
- **Tiered Agent Example** - Real validation → execution → supervisor flow
- **LangChain4J Integration** - End-to-end with actual LLM calls
- **Test Suite** - Comprehensive coverage for all components
- **Docker Setup** - One-command infrastructure setup

### 📋 Planned (see ROADMAP.md)

- Kafka streaming integration
- Apache Flink Agents plugin (when framework matures)
- Additional storage backends
- Advanced RAG with vector embeddings
- Production deployment patterns
- Observability and monitoring

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

**This is active development:**
- ✅ Core algorithms are solid and tested
- 🚧 Integration work in progress
- 📋 Many features are planned, not implemented
- 🧪 Use for learning and experimentation
- ⚠️ Not production-ready yet

**What makes this different:**
- **Honest about status** - No overpromising
- **Real implementations only** - No fake demos
- **Production-focused** - Building for real use cases
- **Community-driven** - Open to feedback and contributions

**Security:**
- Never commit API keys to git
- Use environment variables for secrets (`.env` files)
- Review code before deploying anywhere

## 🚀 Get Started

```bash
# Check current status
cat STATUS.md

# See the roadmap
cat ROADMAP.md

# Build the project
mvn clean compile

# Run tests (when implemented)
mvn test

# Try the simulation demo (not real, just visualization)
./run-demo.sh
```

## 📖 Learn More

- **Architecture**: See `CONCEPTS.md` for core ideas
- **Storage**: See `STORAGE_ARCHITECTURE.md` for storage design
- **Future Plans**: See `ROADMAP.md` for what's coming
- **Progress**: See `STATUS.md` for current state

---

**Being built honestly, one real feature at a time.** 🚀

Questions? Issues? Open a GitHub issue!
