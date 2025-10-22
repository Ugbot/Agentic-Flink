package com.ververica.flink.agent.test;

import com.ververica.flink.agent.example.SimpleCalculatorTool;
import com.ververica.flink.agent.execution.LLMClient;
import com.ververica.flink.agent.execution.LLMResponse;
import com.ververica.flink.agent.execution.ToolCall;
import com.ververica.flink.agent.tool.ToolRegistry;
import com.ververica.flink.agent.tools.ToolExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standalone test for LLM + Tool Execution.
 *
 * <p>Tests the complete agentic loop: LLM → Tool Call → Execution
 *
 * @author Agentic Flink Team
 */
public class StandaloneToolExecutionTest {

  public static void main(String[] args) throws Exception {
    System.out.println("=".repeat(80));
    System.out.println("  Standalone Tool Execution Test");
    System.out.println("=".repeat(80));
    System.out.println();

    // Setup
    LLMClient llmClient = LLMClient.builder()
        .withModel("qwen2.5:latest")
        .withTemperature(0.3)
        .withBaseUrl("http://localhost:11434")
        .build();

    ToolRegistry toolRegistry = ToolRegistry.builder()
        .registerTool("calculator-add", new SimpleCalculatorTool("add"))
        .registerTool("calculator-multiply", new SimpleCalculatorTool("multiply"))
        .build();

    System.out.println("✅ Setup complete");
    System.out.println("   - LLM: qwen2.5:latest");
    System.out.println("   - Tools: " + toolRegistry.getToolNames());
    System.out.println();

    // Test: Multi-step calculation with tools
    testMultiStepCalculation(llmClient, toolRegistry);

    System.out.println("=".repeat(80));
    System.out.println("  Test completed successfully! ✅");
    System.out.println("=".repeat(80));
  }

  private static void testMultiStepCalculation(LLMClient llmClient, ToolRegistry toolRegistry) throws Exception {
    System.out.println("🧪 Test: Multi-step Calculation");
    System.out.println("-".repeat(80));
    System.out.println();

    // Build conversation
    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content",
        "You are a calculator assistant. You have these tools:\n" +
        "- calculator-add: Add two numbers using {\"a\": X, \"b\": Y}\n" +
        "- calculator-multiply: Multiply two numbers using {\"a\": X, \"b\": Y}\n\n" +
        "When you need to calculate, use:\n" +
        "TOOL_CALL: calculator-add {\"a\": 5, \"b\": 3}\n\n" +
        "Show your reasoning."));

    messages.add(Map.of("role", "user", "content",
        "Calculate: (5 + 3) * 2\nDo this step by step using the tools."));

    System.out.println("📤 User: Calculate (5 + 3) * 2 step by step");
    System.out.println();

    // Iteration 1: LLM decides to add first
    System.out.println("🔄 Iteration 1: LLM Planning");
    LLMResponse response1 = llmClient.chat(messages);
    System.out.println("💭 LLM: " + response1.getText().substring(0, Math.min(150, response1.getText().length())) + "...");
    System.out.println();

    if (response1.hasToolCalls()) {
      ToolCall toolCall = response1.getToolCalls().get(0);
      System.out.println("🔧 Tool Call: " + toolCall.getToolName() + " " + toolCall.getParameters());

      // Execute tool
      ToolExecutor executor = toolRegistry.getExecutor(toolCall.getToolName()).get();
      Object result = executor.execute(toolCall.getParameters()).get();
      System.out.println("✅ Tool Result: " + result);
      System.out.println();

      // Add tool result to conversation
      messages.add(Map.of("role", "assistant", "content", response1.getText()));
      messages.add(Map.of("role", "tool", "content", "Result: " + result));

      // Iteration 2: LLM uses result and calls next tool
      System.out.println("🔄 Iteration 2: LLM Continues");
      LLMResponse response2 = llmClient.chat(messages);
      System.out.println("💭 LLM: " + response2.getText().substring(0, Math.min(150, response2.getText().length())) + "...");
      System.out.println();

      if (response2.hasToolCalls()) {
        ToolCall toolCall2 = response2.getToolCalls().get(0);
        System.out.println("🔧 Tool Call: " + toolCall2.getToolName() + " " + toolCall2.getParameters());

        // Execute second tool
        ToolExecutor executor2 = toolRegistry.getExecutor(toolCall2.getToolName()).get();
        Object result2 = executor2.execute(toolCall2.getParameters()).get();
        System.out.println("✅ Tool Result: " + result2);
        System.out.println();

        // Add to conversation
        messages.add(Map.of("role", "assistant", "content", response2.getText()));
        messages.add(Map.of("role", "tool", "content", "Result: " + result2));

        // Iteration 3: LLM provides final answer
        System.out.println("🔄 Iteration 3: Final Answer");
        LLMResponse response3 = llmClient.chat(messages);
        System.out.println("💭 LLM: " + response3.getText());
        System.out.println();
      }
    }

    System.out.println("✅ Multi-step calculation completed!");
    System.out.println();
  }
}
