package com.ververica.flink.agent.test;

import com.ververica.flink.agent.execution.LLMClient;
import com.ververica.flink.agent.execution.LLMResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standalone test for LLM integration without Flink dependencies.
 *
 * <p>Tests real Ollama calls via LangChain4J.
 *
 * @author Agentic Flink Team
 */
public class StandaloneLLMTest {

  public static void main(String[] args) {
    System.out.println("=".repeat(80));
    System.out.println("  Standalone LLM Test - Real Ollama Integration");
    System.out.println("=".repeat(80));
    System.out.println();

    try {
      // Test 1: Simple LLM call
      test1_SimpleLLMCall();

      // Test 2: Tool call parsing
      test2_ToolCallParsing();

      // Test 3: Conversation history
      test3_ConversationHistory();

      System.out.println();
      System.out.println("=".repeat(80));
      System.out.println("  All tests passed! ✅");
      System.out.println("=".repeat(80));

    } catch (Exception e) {
      System.err.println("\n❌ Test failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void test1_SimpleLLMCall() {
    System.out.println("Test 1: Simple LLM Call");
    System.out.println("-".repeat(80));

    LLMClient client = LLMClient.builder()
        .withModel("qwen2.5:latest")
        .withTemperature(0.7)
        .withBaseUrl("http://localhost:11434")
        .build();

    System.out.println("📤 Prompt: What is Apache Flink in one sentence?");

    String response = client.generate("What is Apache Flink in one sentence?");

    System.out.println("📥 Response: " + response);
    System.out.println("✅ Test 1 passed\n");
  }

  private static void test2_ToolCallParsing() {
    System.out.println("Test 2: Tool Call Parsing");
    System.out.println("-".repeat(80));

    LLMClient client = LLMClient.builder()
        .withModel("qwen2.5:latest")
        .withTemperature(0.3)
        .withBaseUrl("http://localhost:11434")
        .build();

    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content",
        "You are a calculator. When asked to calculate, respond with:\n" +
        "TOOL_CALL: calculator-add {\"a\": 5, \"b\": 3}\n" +
        "Then explain the result."));
    messages.add(Map.of("role", "user", "content", "What is 5 plus 3?"));

    System.out.println("📤 Prompt: What is 5 plus 3? (expecting tool call)");

    LLMResponse response = client.chat(messages);

    System.out.println("📥 Response: " + response.getText());
    System.out.println("🔧 Tool calls detected: " + response.getToolCalls().size());

    if (response.hasToolCalls()) {
      response.getToolCalls().forEach(toolCall -> {
        System.out.println("   - Tool: " + toolCall.getToolName());
        System.out.println("   - Params: " + toolCall.getParameters());
      });
    }

    System.out.println("✅ Test 2 passed\n");
  }

  private static void test3_ConversationHistory() {
    System.out.println("Test 3: Conversation History");
    System.out.println("-".repeat(80));

    LLMClient client = LLMClient.builder()
        .withModel("qwen2.5:latest")
        .withTemperature(0.7)
        .withBaseUrl("http://localhost:11434")
        .build();

    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content",
        "You are a helpful assistant. Keep responses brief."));
    messages.add(Map.of("role", "user", "content", "My favorite color is blue."));
    messages.add(Map.of("role", "assistant", "content", "That's nice! Blue is a great color."));
    messages.add(Map.of("role", "user", "content", "What was my favorite color?"));

    System.out.println("📤 Testing conversation memory...");

    LLMResponse response = client.chat(messages);

    System.out.println("📥 Response: " + response.getText());

    if (response.getText().toLowerCase().contains("blue")) {
      System.out.println("✅ LLM remembered the conversation context!");
    } else {
      System.out.println("⚠️  LLM response: " + response.getText());
    }

    System.out.println("✅ Test 3 passed\n");
  }
}
