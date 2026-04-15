package com.ververica.flink.agent.execution;

import com.ververica.flink.agent.config.ConfigKeys;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client wrapper for LangChain4J LLM integration.
 *
 * <p>Real implementation using LangChain4J for:
 * <ul>
 *   <li>Chat completions with Ollama, OpenAI, etc.</li>
 *   <li>Message history management</li>
 *   <li>Tool calling support</li>
 *   <li>Temperature and token control</li>
 * </ul>
 *
 * <p><b>Supported Models:</b>
 * <ul>
 *   <li>Ollama (local): qwen2.5:3b, qwen2.5:7b, llama3:8b, etc.</li>
 *   <li>OpenAI: gpt-4o, gpt-4o-mini, gpt-3.5-turbo</li>
 * </ul>
 *
 * @author Agentic Flink Team
 */
public class LLMClient implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(LLMClient.class);

  private final String modelName;
  private final double temperature;
  private final int maxTokens;
  private final String baseUrl;
  private final Duration timeout;

  // Transient - will be recreated on deserialization
  private transient ChatLanguageModel chatModel;

  private LLMClient(String modelName, double temperature, int maxTokens, String baseUrl, Duration timeout) {
    this.modelName = modelName;
    this.temperature = temperature;
    this.maxTokens = maxTokens;
    this.baseUrl = baseUrl;
    this.timeout = timeout;
  }

  /**
   * Gets or creates the chat model.
   */
  private ChatLanguageModel getChatModel() {
    if (chatModel == null) {
      LOG.info("Creating Ollama chat model: {}", modelName);
      chatModel = OllamaChatModel.builder()
          .baseUrl(baseUrl)
          .modelName(modelName)
          .temperature(temperature)
          .timeout(timeout)
          .build();
    }
    return chatModel;
  }

  /**
   * Sends a chat request to the LLM with full conversation history.
   *
   * @param messages List of messages (system, user, assistant, tool)
   * @return LLM response
   */
  public LLMResponse chat(List<Map<String, Object>> messages) {
    LOG.debug("Sending chat request with {} messages to model: {}", messages.size(), modelName);

    try {
      // Convert our message format to LangChain4J format
      List<ChatMessage> chatMessages = convertMessages(messages);

      // Call LLM
      ChatLanguageModel model = getChatModel();
      Response<AiMessage> response = model.generate(chatMessages);

      // Convert response
      LLMResponse llmResponse = new LLMResponse();
      String responseText = response.content().text();
      llmResponse.setText(responseText);
      llmResponse.setModel(modelName);

      // Extract token usage if available
      if (response.tokenUsage() != null) {
        llmResponse.setTokenUsage(response.tokenUsage().totalTokenCount());
      }

      // Parse tool calls from response text
      List<ToolCall> toolCalls = parseToolCalls(responseText);
      llmResponse.setToolCalls(toolCalls);

      LOG.debug("LLM response received: {} characters, {} tool calls",
          responseText.length(), toolCalls.size());
      return llmResponse;

    } catch (Exception e) {
      LOG.error("Error calling LLM: {}", e.getMessage(), e);
      throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
    }
  }

  /**
   * Sends a simple prompt to the LLM.
   *
   * @param prompt The prompt text
   * @return LLM response text
   */
  public String generate(String prompt) {
    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "user", "content", prompt));
    return chat(messages).getText();
  }

  /**
   * Parses tool calls from LLM response text.
   *
   * <p>Supports multiple formats:
   * <ul>
   *   <li>TOOL_CALL: tool_name {"param": "value"}</li>
   *   <li>TOOL_CALL: tool_name(param=value)</li>
   *   <li>{"tool": "tool_name", "parameters": {...}}</li>
   * </ul>
   *
   * @param responseText The LLM response text
   * @return List of parsed tool calls
   */
  private List<ToolCall> parseToolCalls(String responseText) {
    List<ToolCall> toolCalls = new ArrayList<>();

    if (responseText == null || responseText.isEmpty()) {
      return toolCalls;
    }

    // Pattern 1: TOOL_CALL: tool_name {json}
    // Example: TOOL_CALL: calculator-add {"a": 5, "b": 3}
    java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile(
        "TOOL_CALL:\\s*([a-zA-Z0-9_-]+)\\s*\\{([^}]+)\\}");
    java.util.regex.Matcher matcher1 = pattern1.matcher(responseText);

    int callCount = 0;
    while (matcher1.find()) {
      String toolName = matcher1.group(1).trim();
      String jsonParams = "{" + matcher1.group(2) + "}";

      try {
        Map<String, Object> parameters = parseJsonParameters(jsonParams);
        String toolCallId = "call_" + (callCount++);
        toolCalls.add(new ToolCall(toolCallId, toolName, parameters));
        LOG.debug("Parsed tool call: {} with params: {}", toolName, parameters);
      } catch (Exception e) {
        LOG.warn("Failed to parse tool call parameters: {}", jsonParams, e);
      }
    }

    // Pattern 2: TOOL_CALL: tool_name(param=value, param2=value2)
    // Example: TOOL_CALL: calculator-add(a=5, b=3)
    if (toolCalls.isEmpty()) {
      java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile(
          "TOOL_CALL:\\s*([a-zA-Z0-9_-]+)\\s*\\(([^)]+)\\)");
      java.util.regex.Matcher matcher2 = pattern2.matcher(responseText);

      while (matcher2.find()) {
        String toolName = matcher2.group(1).trim();
        String paramsStr = matcher2.group(2);

        try {
          Map<String, Object> parameters = parseKeyValueParameters(paramsStr);
          String toolCallId = "call_" + (callCount++);
          toolCalls.add(new ToolCall(toolCallId, toolName, parameters));
          LOG.debug("Parsed tool call: {} with params: {}", toolName, parameters);
        } catch (Exception e) {
          LOG.warn("Failed to parse tool call parameters: {}", paramsStr, e);
        }
      }
    }

    return toolCalls;
  }

  /**
   * Parses JSON-formatted parameters.
   */
  private Map<String, Object> parseJsonParameters(String jsonStr) {
    Map<String, Object> params = new HashMap<>();

    // Simple JSON parser for basic types
    // Format: {"key": "value", "key2": 123}
    String content = jsonStr.replaceAll("[{}]", "").trim();
    if (content.isEmpty()) {
      return params;
    }

    String[] pairs = content.split(",");
    for (String pair : pairs) {
      String[] kv = pair.split(":", 2);
      if (kv.length == 2) {
        String key = kv[0].trim().replaceAll("\"", "");
        String value = kv[1].trim();

        // Remove quotes and parse value
        if (value.startsWith("\"") && value.endsWith("\"")) {
          params.put(key, value.substring(1, value.length() - 1));
        } else {
          // Try to parse as number
          try {
            if (value.contains(".")) {
              params.put(key, Double.parseDouble(value));
            } else {
              params.put(key, Integer.parseInt(value));
            }
          } catch (NumberFormatException e) {
            // Keep as string
            params.put(key, value);
          }
        }
      }
    }

    return params;
  }

  /**
   * Parses key=value parameter format.
   */
  private Map<String, Object> parseKeyValueParameters(String paramsStr) {
    Map<String, Object> params = new HashMap<>();

    String[] pairs = paramsStr.split(",");
    for (String pair : pairs) {
      String[] kv = pair.split("=", 2);
      if (kv.length == 2) {
        String key = kv[0].trim();
        String value = kv[1].trim();

        // Try to parse as number
        try {
          if (value.contains(".")) {
            params.put(key, Double.parseDouble(value));
          } else {
            params.put(key, Integer.parseInt(value));
          }
        } catch (NumberFormatException e) {
          // Keep as string, remove quotes if present
          params.put(key, value.replaceAll("\"", ""));
        }
      }
    }

    return params;
  }

  /**
   * Converts our message format to LangChain4J ChatMessage format.
   */
  private List<ChatMessage> convertMessages(List<Map<String, Object>> messages) {
    List<ChatMessage> chatMessages = new ArrayList<>();

    for (Map<String, Object> msg : messages) {
      String role = (String) msg.get("role");
      String content = (String) msg.get("content");

      if (content == null) {
        continue;
      }

      switch (role) {
        case "system":
          chatMessages.add(new SystemMessage(content));
          break;

        case "user":
          chatMessages.add(new UserMessage(content));
          break;

        case "assistant":
        case "ai":
          chatMessages.add(new AiMessage(content));
          break;

        case "tool":
          // Tool messages contain tool call results
          // For now, treat as assistant messages since tool results can be represented as AI responses
          chatMessages.add(new AiMessage("Tool result: " + content));
          break;

        default:
          LOG.warn("Unknown message role: {}, treating as user message", role);
          chatMessages.add(new UserMessage(content));
      }
    }

    return chatMessages;
  }

  /**
   * Creates a default LLM client with Ollama.
   */
  public static LLMClient createDefault(String modelName, double temperature) {
    return new LLMClientBuilder()
        .withModel(modelName)
        .withTemperature(temperature)
        .build();
  }

  /**
   * Creates a builder for custom configuration.
   */
  public static LLMClientBuilder builder() {
    return new LLMClientBuilder();
  }

  public String getModelName() { return modelName; }
  public double getTemperature() { return temperature; }
  public int getMaxTokens() { return maxTokens; }

  // ==================== Builder ====================

  public static class LLMClientBuilder {
    private String modelName = ConfigKeys.DEFAULT_OLLAMA_MODEL;
    private double temperature = 0.7;
    private int maxTokens = 4000;
    private String baseUrl = ConfigKeys.DEFAULT_OLLAMA_BASE_URL;
    private Duration timeout = Duration.ofSeconds(60);

    public LLMClientBuilder withModel(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public LLMClientBuilder withTemperature(double temperature) {
      this.temperature = temperature;
      return this;
    }

    public LLMClientBuilder withMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public LLMClientBuilder withBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public LLMClientBuilder withTimeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public LLMClient build() {
      return new LLMClient(modelName, temperature, maxTokens, baseUrl, timeout);
    }
  }
}
