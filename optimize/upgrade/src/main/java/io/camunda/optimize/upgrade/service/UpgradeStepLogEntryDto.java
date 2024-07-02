/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.upgrade.steps.UpgradeStepType;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Builder
@FieldNameConstants
public class UpgradeStepLogEntryDto {
  @NonNull private String indexName;
  @NonNull private String optimizeVersion;
  @NonNull private UpgradeStepType stepType;
  @NonNull private Integer stepNumber;
  private Instant appliedDate;

  @JsonIgnore
  public String getId() {
    return String.join("_", optimizeVersion, stepType.toString(), indexName);
  }
}
