package com.ververica.flink.agent.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent implements Serializable {

  private String flowId;
  private String userId;
  private String agentId;
  private AgentEventType eventType;
  private Long timestamp;

  // Payload data
  private Map<String, Object> data;

  // Execution context
  private String currentStage;
  private Integer iterationNumber;

  // Routing hints
  private String sourcePattern;
  private String targetPattern;

  // Error information
  private String errorMessage;
  private String errorCode;

  public AgentEvent(String flowId, String userId, String agentId, AgentEventType eventType) {
    this.flowId = flowId;
    this.userId = userId;
    this.agentId = agentId;
    this.eventType = eventType;
    this.timestamp = System.currentTimeMillis();
    this.data = new HashMap<>();
    this.iterationNumber = 0;
  }

  public void putData(String key, Object value) {
    if (this.data == null) {
      this.data = new HashMap<>();
    }
    this.data.put(key, value);
  }

  public Object getData(String key) {
    return this.data != null ? this.data.get(key) : null;
  }

  public <T> T getData(String key, Class<T> type) {
    Object value = getData(key);
    if (value != null && type.isInstance(value)) {
      return type.cast(value);
    }
    return null;
  }

  public boolean hasError() {
    return errorMessage != null || errorCode != null;
  }
}
