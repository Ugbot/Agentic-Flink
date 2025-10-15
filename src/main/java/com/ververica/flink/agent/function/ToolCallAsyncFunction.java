package com.ververica.flink.agent.function;

import com.ververica.flink.agent.core.AgentEvent;
import com.ververica.flink.agent.core.AgentEventType;
import com.ververica.flink.agent.core.ToolDefinition;
import com.ververica.flink.agent.serde.ToolCallRequest;
import com.ververica.flink.agent.serde.ToolCallResponse;
import com.ververica.flink.agent.langchain.client.LangChainAsyncClient;
import com.ververica.flink.agent.langchain.model.AiModel;
import com.ververica.flink.agent.langchain.model.language.LangChainLanguageModel;
import com.ververica.flink.agent.langchain.model.language.OllamaLanguageModel;
import com.ververica.flink.agent.langchain.model.language.OpenAiLanguageModel;
import com.ververica.flink.agent.langchain.config.LLMConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolCallAsyncFunction extends RichAsyncFunction<ToolCallRequest, ToolCallResponse> {

  private static final Logger LOG = LoggerFactory.getLogger(ToolCallAsyncFunction.class);
  public static final String UID = ToolCallAsyncFunction.class.getSimpleName();

  private transient LangChainAsyncClient langChainAsyncClient;
  private final Map<String, ToolDefinition> toolRegistry;

  public ToolCallAsyncFunction(Map<String, ToolDefinition> toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    this.langChainAsyncClient =
        new LangChainAsyncClient(
            List.of(
                LangChainLanguageModel.DEFAULT_MODEL,
                new OllamaLanguageModel(),
                new OpenAiLanguageModel()));
  }

  @Override
  public void asyncInvoke(
      ToolCallRequest request, ResultFuture<ToolCallResponse> resultFuture) {

    LOG.info(
        "Executing tool call for flow {}, tool: {}", request.getFlowId(), request.getToolId());

    // Get tool definition
    ToolDefinition toolDef = toolRegistry.get(request.getToolId());
    if (toolDef == null) {
      ToolCallResponse response = new ToolCallResponse();
      response.setRequestId(request.getRequestId());
      response.setFlowId(request.getFlowId());
      response.setUserId(request.getUserId());
      response.setAgentId(request.getAgentId());
      response.setToolId(request.getToolId());
      response.fail("Tool not found in registry: " + request.getToolId(), "TOOL_NOT_FOUND");
      resultFuture.complete(Collections.singleton(response));
      return;
    }

    // Build prompt for tool execution
    String toolPrompt = buildToolExecutionPrompt(toolDef, request.getParameters());
    List<ChatMessage> messages = new ArrayList<>();
    messages.add(new UserMessage(toolPrompt));

    // Create LLM config from tool definition
    LLMConfig llmConfig = createLLMConfig(request, toolDef);

    // Execute async
    CompletableFuture<Response<AiMessage>> asyncResponse =
        langChainAsyncClient.generate(messages, llmConfig);

    processAsyncResponse(request, toolDef, resultFuture, asyncResponse);
  }

  @Override
  public void timeout(ToolCallRequest input, ResultFuture<ToolCallResponse> resultFuture) {
    LOG.warn("Tool call timed out for flow {}, tool: {}", input.getFlowId(), input.getToolId());

    ToolCallResponse response =
        new ToolCallResponse(
            input.getRequestId(), input.getFlowId(), input.getUserId(), input.getAgentId());
    response.setToolId(input.getToolId());
    response.setToolName(input.getToolName());
    response.fail("Tool execution timed out", "TIMEOUT");

    resultFuture.complete(Collections.singleton(response));
  }

  private String buildToolExecutionPrompt(
      ToolDefinition toolDef, Map<String, Object> parameters) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Execute the following tool:\n\n");
    prompt.append("Tool: ").append(toolDef.getName()).append("\n");
    prompt.append("Description: ").append(toolDef.getDescription()).append("\n");
    prompt.append("Parameters:\n");

    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      prompt.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }

    prompt.append("\nProvide the result of executing this tool.");
    return prompt.toString();
  }

  private LLMConfig createLLMConfig(ToolCallRequest request, ToolDefinition toolDef) {
    LLMConfig config = new LLMConfig();
    config.setUserId(Long.valueOf(request.getUserId().hashCode()));
    config.setAiModel(AiModel.OLLAMA); // Default, should be configurable
    config.setSystemMessage("You are a tool execution assistant. Execute tools precisely and return structured results.");

    // Copy executor config from tool definition
    if (toolDef.getExecutorConfig() != null) {
      config.setProperties(toolDef.getExecutorConfig());
    }

    return config;
  }

  private void processAsyncResponse(
      ToolCallRequest request,
      ToolDefinition toolDef,
      ResultFuture<ToolCallResponse> resultFuture,
      CompletableFuture<Response<AiMessage>> asyncResponse) {

    asyncResponse.whenComplete(
        (result, throwable) -> {
          ToolCallResponse response =
              new ToolCallResponse(
                  request.getRequestId(),
                  request.getFlowId(),
                  request.getUserId(),
                  request.getAgentId());
          response.setToolId(request.getToolId());
          response.setToolName(toolDef.getName());

          if (throwable != null) {
            LOG.error(
                "Tool execution failed for flow {}, tool: {}",
                request.getFlowId(),
                request.getToolId(),
                throwable);
            response.fail(throwable.getMessage(), "EXECUTION_ERROR");
          } else {
            response.complete(result.content().text());
            LOG.info(
                "Tool execution completed for flow {}, tool: {}",
                request.getFlowId(),
                request.getToolId());
          }

          resultFuture.complete(Collections.singleton(response));
        });
  }

  public static AgentEvent responseToEvent(ToolCallResponse response) {
    AgentEvent event = new AgentEvent();
    event.setFlowId(response.getFlowId());
    event.setUserId(response.getUserId());
    event.setAgentId(response.getAgentId());
    event.setEventType(
        response.isSuccess()
            ? AgentEventType.TOOL_CALL_COMPLETED
            : AgentEventType.TOOL_CALL_FAILED);
    event.setTimestamp(System.currentTimeMillis());
    event.putData("toolId", response.getToolId());
    event.putData("result", response.getResult());
    event.putData("success", response.isSuccess());
    if (!response.isSuccess()) {
      event.setErrorMessage(response.getErrorMessage());
      event.setErrorCode(response.getErrorCode());
    }
    return event;
  }
}
