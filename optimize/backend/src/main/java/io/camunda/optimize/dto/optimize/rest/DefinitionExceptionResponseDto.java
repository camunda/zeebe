/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefinitionExceptionResponseDto extends ErrorResponseDto {

  private Set<DefinitionExceptionItemDto> definitions;

  protected DefinitionExceptionResponseDto() {
    this(null, null, null, Collections.emptySet());
  }

  public DefinitionExceptionResponseDto(
      final String errorCode,
      final String errorMessage,
      final String detailedErrorMessage,
      final Set<DefinitionExceptionItemDto> definitions) {
    super(errorCode, errorMessage, detailedErrorMessage);
    this.definitions = definitions;
  }
}
