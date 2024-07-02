/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class HistoricUserTaskInstanceDto implements TenantSpecificEngineDto {
  private String id; // == FlowNodeInstanceDto.userTaskId
  private String processDefinitionKey;
  private String processDefinitionId;
  private String processInstanceId;
  private String executionId;
  private String caseDefinitionKey;
  private String caseDefinitionId;
  private String caseInstanceId;
  private String caseExecutionId;
  private String activityInstanceId; // == FlowNodeInstanceDto.flowNodeInstanceId
  private String name;
  private String description;
  private String deleteReason;
  private String owner;
  private String assignee;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private Long duration;
  private String taskDefinitionKey; // == FlowNodeInstanceDto.flowNodeId
  private int priority;
  private OffsetDateTime due;
  private String parentTaskId;
  private OffsetDateTime followUp;
  private String tenantId;
  private OffsetDateTime removalTime;
  private String rootProcessInstanceId;

  @Override
  public Optional<String> getTenantId() {
    return Optional.ofNullable(tenantId);
  }
}
