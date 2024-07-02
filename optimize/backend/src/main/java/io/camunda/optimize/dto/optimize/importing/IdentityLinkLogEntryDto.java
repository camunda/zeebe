/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.importing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.dto.optimize.OptimizeDto;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

@Accessors(chain = true)
@AllArgsConstructor
@Data
@FieldNameConstants(asEnum = true)
public class IdentityLinkLogEntryDto implements OptimizeDto {

  private String id;

  @JsonIgnore private String processInstanceId;
  @JsonIgnore private String processDefinitionKey;
  @JsonIgnore private String engine;

  private IdentityLinkLogType type;
  private String userId;
  private String groupId;
  private String taskId; // == userTaskId
  private String operationType;
  private String assignerId;
  private OffsetDateTime timestamp;
}
