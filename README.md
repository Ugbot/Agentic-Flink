# 🤖 Agentic Flink - Build Smart AI Agents with Apache Flink

> **Easy-to-use framework for creating autonomous AI agents that run on Apache Flink**

Think of this as "AI agents that never forget, always recover, and scale automatically."

## 📖 What Is This?

Agentic Flink lets you build **AI agents** - autonomous programs that can:
- 🧠 Make decisions using AI (like ChatGPT/Ollama)
- 🔧 Use tools (search databases, call APIs, process data)
- 💾 Remember important information (short-term and long-term memory)
- ✅ Validate their work and correct mistakes
- 🔄 Handle errors gracefully and try again
- 📈 Scale to handle millions of requests

**Perfect for:**
- Customer service bots that remember conversations
- Data processing pipelines that make intelligent decisions
- Automated workflows that adapt to changing conditions
- Any AI application that needs to be reliable and scalable

## 🎯 Why Use This Framework?

| Feature | What It Means |
|---------|---------------|
| **Built on Apache Flink** | Your agents never lose data, even if servers crash |
| **Context Management** | Agents remember what's important and forget what's not |
| **RAG Built-In** | Connect your AI to your documents and databases |
| **Tool Framework** | Easily add new capabilities to your agents |
| **Beginner Friendly** | Clear examples and extensive documentation |

## 🚀 5-Minute Quick Start

### Step 1: Prerequisites

You'll need three things installed:

1. **Java 11 or higher** - [Download here](https://adoptium.net/)
   ```bash
   java -version  # Should show 11 or higher
   ```

2. **Maven** - [Download here](https://maven.apache.org/download.cgi)
   ```bash
   mvn -version  # Should show 3.6 or higher
   ```

3. **Ollama** - [Download here](https://ollama.ai)
   ```bash
   ollama --version  # Should show the version
   ```

### Step 2: Start Ollama (Your Local AI)

Open a terminal and run:

```bash
# Start Ollama in the background
ollama serve

# In another terminal, download the AI models
ollama pull llama2:latest        # Language model for conversations
ollama pull nomic-embed-text     # Embedding model for understanding text
```

💡 **What's happening?** Ollama is like having ChatGPT running on your own computer!

### Step 3: Build the Project

```bash
cd /Users/bengamble/Agentic-Flink
mvn clean package
```

This takes about 30 seconds and creates a file you can run.

### Step 4: Run Your First Agent!

```bash
java -cp target/agentic-flink-1.0.0-SNAPSHOT.jar \
  com.ververica.flink.agent.example.SimpleAgentExample
```

🎉 **You just ran an AI agent!** Check the output to see it:
- Receive tasks
- Call tools (like a calculator)
- Validate results
- Complete the workflow

## 📚 What Can I Build?

### Example 1: Customer Support Bot

```java
// An agent that helps customers and remembers conversations
AgentConfig config = new AgentConfig();
config.setAgentId("customer-support-bot");
config.setMaxIterations(5);  // Try up to 5 times to solve the problem

// Give it tools
config.addTool(new DatabaseSearchTool());     // Search customer history
config.addTool(new EmailTool());              // Send responses
config.addTool(new KnowledgeBaseTool());      // Look up solutions
```

### Example 2: Document Processor

```java
// An agent that reads documents and extracts information
AgentConfig config = new AgentConfig();
config.setAgentId("doc-processor");

// It can ingest documents and search through them
config.addTool(new DocumentIngestionTool());
config.addTool(new SemanticSearchTool());
config.addTool(new RagQueryTool());
```

### Example 3: Data Pipeline

```java
// An agent that processes data and makes decisions
AgentConfig config = new AgentConfig();
config.setAgentId("data-pipeline");

// It validates data quality and takes action
config.addTool(new DataValidationTool());
config.addTool(new TransformationTool());
config.addTool(new AlertingTool());
```

## 🎓 Learning Path

**Start here:** → [Getting Started Guide](GETTING_STARTED.md)
- Step-by-step setup
- Your first agent
- Understanding the output

**Then learn:** → [Core Concepts](CONCEPTS.md)
- What is an agent?
- How do tools work?
- Memory and context
- Validation and correction

**Try examples:** → [Example Walkthroughs](EXAMPLES.md)
- Simple Agent: Basic workflow
- RAG Agent: Work with documents
- Context Agent: Memory management

**Deep dive:** → [Framework Documentation](AGENT_FRAMEWORK.md)
- Advanced patterns
- Custom tools
- Production deployment

## 🔑 Core Concepts (Simple Explanations)

### What's an Agent?

An **agent** is like a smart worker that:
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

**Tools** are things your agent can do:
- 🔍 Search a database
- 📧 Send an email
- 🧮 Calculate something
- 📄 Read a document
- 🌐 Call an API

You can easily add your own tools!

### What's Context Management?

Imagine your agent talking to a user. After 1000 messages, it can't remember everything. **Context management** automatically:
- Keeps important facts (MUST)
- Compresses useful info (SHOULD)
- Forgets irrelevant stuff (COULD/WONT)

It's like having a smart notebook that highlights important things.

### What's RAG?

**RAG** (Retrieval Augmented Generation) lets your AI know about YOUR data:

```
Your Documents → Break into chunks → Store embeddings → Search when needed
```

Instead of the AI just guessing, it can look up facts from your documents!

## 📦 Project Structure (Explained)

```
Agentic-Flink/
├── README.md                    ← You are here!
├── GETTING_STARTED.md           ← Start your journey
├── CONCEPTS.md                  ← Understand the framework
├── EXAMPLES.md                  ← Learn by doing
├── pom.xml                      ← Maven build file (you rarely touch this)
│
├── src/main/java/.../agent/
│   ├── core/                    ← Basic building blocks
│   │   ├── AgentEvent.java      ← Events that agents process
│   │   ├── ToolDefinition.java  ← How to define a tool
│   │   └── AgentConfig.java     ← Configure your agent
│   │
│   ├── tools/                   ← Where you add new tools
│   │   ├── ToolExecutor.java    ← Base for all tools
│   │   └── rag/                 ← Document search tools
│   │
│   ├── context/                 ← Memory management
│   │   ├── core/                ← Context primitives
│   │   ├── memory/              ← Short/long term memory
│   │   └── compaction/          ← Automatic cleanup
│   │
│   ├── langchain/               ← AI model integration
│   │   ├── client/              ← Talk to Ollama/OpenAI
│   │   └── model/               ← Different AI models
│   │
│   └── example/                 ← Ready-to-run examples
│       ├── SimpleAgentExample.java
│       ├── RagAgentExample.java
│       └── ContextManagementExample.java
```

## 🛠️ Common Tasks

### Run Different Examples

```bash
# Basic agent workflow
java -cp target/agentic-flink-1.0.0-SNAPSHOT.jar \
  com.ververica.flink.agent.example.SimpleAgentExample

# Work with documents (RAG)
java -cp target/agentic-flink-1.0.0-SNAPSHOT.jar \
  com.ververica.flink.agent.example.RagAgentExample

# Memory management
java -cp target/agentic-flink-1.0.0-SNAPSHOT.jar \
  com.ververica.flink.agent.example.ContextManagementExample
```

### Change the AI Model

Edit the example file and change:

```java
LLMConfig config = new LLMConfig();
config.setAiModel(AiModel.OLLAMA);  // or AiModel.OPENAI

Map<String, String> properties = new HashMap<>();
properties.put("modelName", "llama2:latest");  // Change model here
```

### Add Your Own Tool

1. Create a new class extending `AbstractToolExecutor`
2. Implement the `execute()` method
3. Register it with your agent

Example:

```java
public class MyCustomTool extends AbstractToolExecutor {
    @Override
    public CompletableFuture<Object> execute(Map<String, Object> params) {
        // Do your work here
        String input = getRequiredParameter(params, "input");
        String result = processInput(input);
        return CompletableFuture.completedFuture(result);
    }
}
```

## ❓ Frequently Asked Questions

### Do I need to know Apache Flink?

**No!** The framework handles all the Flink complexity. You just write agent logic.

### Do I need expensive AI APIs?

**No!** Use Ollama to run AI models locally for free.

### Can I use this in production?

**Yes!** Built on Apache Flink, it's production-ready with:
- Exactly-once processing guarantees
- Automatic scaling
- Failure recovery
- State management

### How do I debug issues?

Check the logs! They show each step:
```
[INFO] Agent agent-001: Received event type=TOOL_CALL_REQUESTED
[INFO] Agent agent-001: Executing tool=calculator
[INFO] Agent agent-001: Validation passed
[INFO] Agent agent-001: Flow completed
```

### Can I connect to my database?

**Yes!** Create a tool that connects to your database:

```java
public class DatabaseTool extends AbstractToolExecutor {
    @Override
    public CompletableFuture<Object> execute(Map<String, Object> params) {
        String query = getRequiredParameter(params, "query");
        // Connect to your database and execute query
        return CompletableFuture.completedFuture(results);
    }
}
```

## 🎯 Next Steps

1. **Run the examples** - See what's possible
2. **Read [Getting Started](GETTING_STARTED.md)** - Build your first agent
3. **Understand [Concepts](CONCEPTS.md)** - Learn how it works
4. **Try [Examples](EXAMPLES.md)** - Learn by doing
5. **Create your own agent** - Build something amazing!

## 🆘 Need Help?

- 📖 **Read the docs** - Start with [Getting Started](GETTING_STARTED.md)
- 🔍 **Check [Troubleshooting](TROUBLESHOOTING.md)** - Common issues and fixes
- 💡 **Look at examples** - See [EXAMPLES.md](EXAMPLES.md)
- 🐛 **Something broken?** - Check the logs, they're very detailed

## 🌟 What Makes This Special?

| Other Frameworks | Agentic Flink |
|------------------|---------------|
| Run on one machine | Scale to thousands of machines |
| Lose data on crash | Never lose data (Flink guarantees) |
| Complex setup | Simple examples to start |
| Manual memory management | Automatic context management |
| Build from scratch | RAG and tools included |

## 📝 License

Apache License 2.0 - Use it freely!

## 🚀 Ready to Build?

```bash
# Start here
cd /Users/bengamble/Agentic-Flink
mvn clean package

# Run your first agent
java -cp target/agentic-flink-1.0.0-SNAPSHOT.jar \
  com.ververica.flink.agent.example.SimpleAgentExample

# Then read the getting started guide
open GETTING_STARTED.md
```

**Welcome to the world of scalable AI agents!** 🎉
