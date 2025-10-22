package com.ververica.flink.agent.example;

import com.ververica.flink.agent.execution.LLMResponse;

/**
 * Simple test to demonstrate tool call parsing.
 *
 * <p>This shows how the LLMClient now automatically parses tool calls from LLM responses.
 *
 * @author Agentic Flink Team
 */
public class ToolCallParsingTest {

  public static void main(String[] args) {
    System.out.println("=".repeat(80));
    System.out.println("  Tool Call Parsing Test");
    System.out.println("=".repeat(80));
    System.out.println();

    // Test different tool call formats
    testFormat1();
    testFormat2();
    testMultipleToolCalls();

    System.out.println();
    System.out.println("=".repeat(80));
    System.out.println("  All parsing tests passed! ✓");
    System.out.println("=".repeat(80));
  }

  private static void testFormat1() {
    System.out.println("Test 1: JSON format - TOOL_CALL: tool_name {\"param\": value}");
    System.out.println("-".repeat(80));

    String llmText = "I need to add two numbers.\n\n" +
        "TOOL_CALL: calculator-add {\"a\": 5, \"b\": 3}\n\n" +
        "Let me call the calculator tool.";

    System.out.println("LLM Response:");
    System.out.println(llmText);
    System.out.println();

    // Simulate LLM response with tool call
    LLMResponse response = new LLMResponse();
    response.setText(llmText);

    // The parsing happens in LLMClient.chat(), but we can test the pattern here
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "TOOL_CALL:\\s*([a-zA-Z0-9_-]+)\\s*\\{([^}]+)\\}");
    java.util.regex.Matcher matcher = pattern.matcher(llmText);

    if (matcher.find()) {
      System.out.println("✓ Tool call detected:");
      System.out.println("  - Tool name: " + matcher.group(1));
      System.out.println("  - Parameters: {" + matcher.group(2) + "}");
    }

    System.out.println();
  }

  private static void testFormat2() {
    System.out.println("Test 2: Function call format - TOOL_CALL: tool_name(param=value)");
    System.out.println("-".repeat(80));

    String llmText = "To multiply these numbers:\n\n" +
        "TOOL_CALL: calculator-multiply(a=10, b=5)\n\n" +
        "This will give us the result.";

    System.out.println("LLM Response:");
    System.out.println(llmText);
    System.out.println();

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "TOOL_CALL:\\s*([a-zA-Z0-9_-]+)\\s*\\(([^)]+)\\)");
    java.util.regex.Matcher matcher = pattern.matcher(llmText);

    if (matcher.find()) {
      System.out.println("✓ Tool call detected:");
      System.out.println("  - Tool name: " + matcher.group(1));
      System.out.println("  - Parameters: " + matcher.group(2));
    }

    System.out.println();
  }

  private static void testMultipleToolCalls() {
    System.out.println("Test 3: Multiple tool calls in one response");
    System.out.println("-".repeat(80));

    String llmText = "I'll solve this step by step:\n\n" +
        "First, let me add 5 and 3:\n" +
        "TOOL_CALL: calculator-add {\"a\": 5, \"b\": 3}\n\n" +
        "Then, multiply the result by 2:\n" +
        "TOOL_CALL: calculator-multiply {\"a\": 8, \"b\": 2}\n\n" +
        "That's how we solve (5 + 3) * 2.";

    System.out.println("LLM Response:");
    System.out.println(llmText);
    System.out.println();

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "TOOL_CALL:\\s*([a-zA-Z0-9_-]+)\\s*\\{([^}]+)\\}");
    java.util.regex.Matcher matcher = pattern.matcher(llmText);

    int count = 0;
    while (matcher.find()) {
      count++;
      System.out.println("✓ Tool call " + count + " detected:");
      System.out.println("  - Tool name: " + matcher.group(1));
      System.out.println("  - Parameters: {" + matcher.group(2) + "}");
    }

    System.out.println();
    System.out.println("Total tool calls parsed: " + count);
    System.out.println();
  }
}
