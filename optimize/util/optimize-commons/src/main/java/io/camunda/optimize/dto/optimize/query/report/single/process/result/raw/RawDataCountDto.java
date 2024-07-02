/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.result.raw;

import io.camunda.optimize.dto.optimize.query.report.single.RawDataInstanceDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor
@AllArgsConstructor
@Data
@FieldNameConstants(asEnum = true)
public class RawDataCountDto implements RawDataInstanceDto {
  protected long incidents;
  protected long openIncidents;
  protected long userTasks;
}
