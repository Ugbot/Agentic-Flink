# Agentic Flink

A framework for building LLM-powered AI agents on Apache Flink. It combines
Flink's stream processing and CEP pattern matching with LangChain4J's LLM
integration to produce agent workflows that scale horizontally, manage context
automatically, and persist state across a two-tier storage backend.

## Key Features

- Stream-native agent orchestration on Apache Flink 1.20
- LangChain4J integration for LLM calls (Ollama, OpenAI)
- MoSCoW context management with 5-phase compaction
- Two-tier storage (Redis hot cache + PostgreSQL durable)
- CEP-based pattern matching for agent workflows
- Extensible tool framework with `@Tool` annotation discovery
- Supervisor chains with N-tier escalation

## Quick Start

```bash
git clone https://github.com/bengambleVV/Agentic-Flink.git
cd Agentic-Flink

# Start infrastructure (PostgreSQL + Redis + Ollama)
# Use `podman compose` if you run Podman instead of Docker
docker compose up -d

# Pull a local LLM
docker compose exec ollama ollama pull qwen2.5:3b

# Run the test suite
mvn clean test

# Run a working example
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.example.QuickStartExample"
```

## Architecture

```
Events (mock / Kafka)
        |
        v
+-------------------------+
|  Flink CEP              |
|  Pattern Matching       |
|  - Validation triggers  |
|  - Execution requests   |
|  - Escalation detection |
+-----------+-------------+
            |
            v
+-------------------------+
|  Agent Loop             |
|  LangChain4J + Tools    |
|  - LLM reasoning        |
|  - Tool execution       |
|  - Prompt management    |
+-----------+-------------+
            |
            v
+-------------------------+
|  Context Management     |
|  MoSCoW Prioritization  |
|  - 5-phase compaction   |
|  - Relevancy scoring    |
+-----------+-------------+
            |
            v
+-------------------------+
|  Two-Tier Storage       |
|  Redis (hot)            |
|  PostgreSQL (durable)   |
+-------------------------+
```

1. Events arrive as a Flink DataStream (from Kafka or mock sources).
2. Flink CEP matches patterns -- validation triggers, execution requests, escalation conditions.
3. LangChain4J calls the configured LLM to decide which tools to invoke.
4. Tools execute real work (calculations, API calls, lookups).
5. MoSCoW context management retains important context and discards noise.
6. Redis caches active data; PostgreSQL stores durable history.

## Examples

The project ships with 13 runnable examples at increasing complexity.

| Level | Example | What it demonstrates |
|-------|---------|----------------------|
| 1 | `QuickStartExample` | Minimal agent with a single tool |
| 2 | `ContextManagementExample` | MoSCoW prioritization and compaction |
| 3 | `TieredAgentExample` | Validation, execution, and supervision chain |
| 4 | `StorageIntegratedFlinkJob` | Full PostgreSQL + Redis persistence |

All examples live in `src/main/java/com/ververica/flink/agent/example/`.
See [docs/reference/examples.md](docs/reference/examples.md) for detailed walkthroughs.

## Documentation

| Document | Description |
|----------|-------------|
| [docs/getting-started.md](docs/getting-started.md) | Setup guide and first steps |
| [docs/guides/context-management.md](docs/guides/context-management.md) | MoSCoW prioritization and compaction algorithms |
| [docs/guides/storage-quickstart.md](docs/guides/storage-quickstart.md) | Storage backend setup and integration |
| [docs/guides/openai-setup.md](docs/guides/openai-setup.md) | Configuring OpenAI as the LLM provider |
| [docs/guides/flink-agents-integration.md](docs/guides/flink-agents-integration.md) | Integration with Apache Flink Agents |
| [docs/reference/agent-framework.md](docs/reference/agent-framework.md) | Framework reference and agent patterns |
| [docs/reference/storage-architecture.md](docs/reference/storage-architecture.md) | Two-tier storage design |
| [docs/reference/troubleshooting.md](docs/reference/troubleshooting.md) | Common issues and fixes |

## In Development

The following features are under active work and not yet functional:

- Advanced CEP patterns for multi-agent coordination
- Performance benchmarking suite
- Vector search with Qdrant (RAG v2)
- Monitoring and observability hooks

See [ROADMAP.md](ROADMAP.md) for the full timeline.

## Requirements

- Java 17+
- Maven 3.8+
- Apache Flink 1.20
- Docker or Podman (for infrastructure services)
- Ollama (for local LLM examples)

## Contributing

Contributions are welcome. Open an issue to report bugs, suggest features, or
ask questions. Pull requests are appreciated -- especially for additional
storage backends and tool implementations.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
