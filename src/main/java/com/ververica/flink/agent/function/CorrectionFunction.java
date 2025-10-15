package com.ververica.flink.agent.function;

import com.ververica.flink.agent.core.AgentEvent;
import com.ververica.flink.agent.core.AgentEventType;
import com.ververica.flink.agent.serde.ValidationResult;
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
import java.util.concurrent.CompletableFuture;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrectionFunction extends RichAsyncFunction<AgentEvent, AgentEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(CorrectionFunction.class);
  public static final String UID = CorrectionFunction.class.getSimpleName();

  private transient LangChainAsyncClient langChainAsyncClient;
  private final String correctionPromptTemplate;
  private final int maxCorrectionAttempts;

  public CorrectionFunction(String correctionPromptTemplate, int maxCorrectionAttempts) {
    this.correctionPromptTemplate =
        correctionPromptTemplate != null
            ? correctionPromptTemplate
            : "The following result failed validation:\n\n"
                + "Original Result: {{result}}\n"
                + "Validation Errors: {{errors}}\n\n"
                + "Please correct the result to address these errors.";
    this.maxCorrectionAttempts = maxCorrectionAttempts;
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
  public void asyncInvoke(AgentEvent event, ResultFuture<AgentEvent> resultFuture) {

    LOG.info("Attempting correction for flow: {}", event.getFlowId());

    // Check if we've exceeded max attempts
    Integer attemptCount = event.getData("correctionAttempts", Integer.class);
    if (attemptCount == null) {
      attemptCount = 0;
    }

    if (attemptCount >= maxCorrectionAttempts) {
      LOG.warn(
          "Max correction attempts reached for flow: {}, escalating to supervisor",
          event.getFlowId());
      AgentEvent supervisorEvent = createSupervisorEscalationEvent(event);
      resultFuture.complete(Collections.singleton(supervisorEvent));
      return;
    }

    // Extract validation result and original result
    ValidationResult validation = event.getData("validationResult", ValidationResult.class);
    Object originalResult = event.getData("originalResult");

    // Build correction prompt
    String correctionPrompt =
        correctionPromptTemplate
            .replace("{{result}}", originalResult != null ? originalResult.toString() : "")
            .replace(
                "{{errors}}",
                validation != null ? String.join(", ", validation.getErrors()) : "Unknown errors");

    List<ChatMessage> messages = new ArrayList<>();
    messages.add(new UserMessage(correctionPrompt));

    // Create LLM config
    LLMConfig llmConfig = createLLMConfig(event);

    // Execute correction async
    CompletableFuture<Response<AiMessage>> asyncResponse =
        langChainAsyncClient.generate(messages, llmConfig);

    processCorrectionResponse(event, attemptCount + 1, resultFuture, asyncResponse);
  }

  @Override
  public void timeout(AgentEvent input, ResultFuture<AgentEvent> resultFuture) {
    LOG.warn("Correction timed out for flow: {}", input.getFlowId());

    // On timeout, escalate to supervisor
    AgentEvent supervisorEvent = createSupervisorEscalationEvent(input);
    resultFuture.complete(Collections.singleton(supervisorEvent));
  }

  private LLMConfig createLLMConfig(AgentEvent event) {
    LLMConfig config = new LLMConfig();
    config.setUserId(Long.valueOf(event.getUserId().hashCode()));
    config.setAiModel(AiModel.OLLAMA);
    config.setSystemMessage(
        "You are a correction assistant. Fix errors in tool execution results based on validation feedback.");
    return config;
  }

  private void processCorrectionResponse(
      AgentEvent event,
      int attemptCount,
      ResultFuture<AgentEvent> resultFuture,
      CompletableFuture<Response<AiMessage>> asyncResponse) {

    asyncResponse.whenComplete(
        (result, throwable) -> {
          if (throwable != null) {
            LOG.error("Correction failed for flow: {}", event.getFlowId(), throwable);
            AgentEvent supervisorEvent = createSupervisorEscalationEvent(event);
            resultFuture.complete(Collections.singleton(supervisorEvent));
            return;
          }

          String correctedResult = result.content().text();
          LOG.info(
              "Correction attempt {} completed for flow: {}", attemptCount, event.getFlowId());

          AgentEvent correctionEvent = new AgentEvent();
          correctionEvent.setFlowId(event.getFlowId());
          correctionEvent.setUserId(event.getUserId());
          correctionEvent.setAgentId(event.getAgentId());
          correctionEvent.setEventType(AgentEventType.CORRECTION_COMPLETED);
          correctionEvent.setTimestamp(System.currentTimeMillis());
          correctionEvent.setCurrentStage(event.getCurrentStage());
          correctionEvent.setIterationNumber(event.getIterationNumber());
          correctionEvent.putData("correctedResult", correctedResult);
          correctionEvent.putData("correctionAttempts", attemptCount);
          correctionEvent.putData("result", correctedResult); // Replace original result

          resultFuture.complete(Collections.singleton(correctionEvent));
        });
  }

  private AgentEvent createSupervisorEscalationEvent(AgentEvent event) {
    AgentEvent supervisorEvent = new AgentEvent();
    supervisorEvent.setFlowId(event.getFlowId());
    supervisorEvent.setUserId(event.getUserId());
    supervisorEvent.setAgentId(event.getAgentId());
    supervisorEvent.setEventType(AgentEventType.SUPERVISOR_REVIEW_REQUESTED);
    supervisorEvent.setTimestamp(System.currentTimeMillis());
    supervisorEvent.setCurrentStage(event.getCurrentStage());
    supervisorEvent.setIterationNumber(event.getIterationNumber());
    supervisorEvent.putData("reason", "Max correction attempts exceeded");
    supervisorEvent.putData("originalResult", event.getData("originalResult"));
    supervisorEvent.putData("validationResult", event.getData("validationResult"));
    return supervisorEvent;
  }
}
