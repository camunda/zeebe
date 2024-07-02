/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest;

import io.camunda.optimize.service.exceptions.OptimizeAlertEmailValidationException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants
public class AlertEmailValidationResponseDto extends ErrorResponseDto {
  private final String invalidAlertEmails;

  public AlertEmailValidationResponseDto(
      final OptimizeAlertEmailValidationException optimizeAlertEmailValidationException) {
    super(
        optimizeAlertEmailValidationException.getErrorCode(),
        optimizeAlertEmailValidationException.getMessage(),
        optimizeAlertEmailValidationException.getMessage());
    this.invalidAlertEmails =
        String.join(", ", optimizeAlertEmailValidationException.getAlertEmails());
  }
}
