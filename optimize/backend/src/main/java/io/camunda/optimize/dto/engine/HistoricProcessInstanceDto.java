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
public class HistoricProcessInstanceDto implements TenantSpecificEngineDto {
  protected String id;
  protected String businessKey;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected Integer processDefinitionVersion;
  protected String processDefinitionName;
  protected OffsetDateTime startTime;
  protected OffsetDateTime endTime;
  protected Long durationInMillis;
  protected String startUserId;
  protected String startActivityId;
  protected String deleteReason;
  protected String superProcessInstanceId;
  protected String superCaseInstanceId;
  protected String caseInstanceId;
  protected String tenantId;
  protected String state;

  public String getProcessDefinitionVersionAsString() {
    return processDefinitionVersion != null ? processDefinitionVersion.toString() : null;
  }

  @Override
  public Optional<String> getTenantId() {
    return Optional.ofNullable(tenantId);
  }
}
