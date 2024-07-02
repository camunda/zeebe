/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.query.variable.VariableType.BOOLEAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.variable.ProcessToQueryDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameResponseDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableValueRequestDto;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class ProcessVariableRestServiceIT extends AbstractPlatformIT {

  @Test
  public void getVariableNamesWithoutAuthentication() {
    // given
    ProcessToQueryDto processToQuery = new ProcessToQueryDto();
    processToQuery.setProcessDefinitionKey("zhoka");
    processToQuery.setProcessDefinitionVersion("1");

    ProcessVariableNameRequestDto variableRequestDto =
        new ProcessVariableNameRequestDto(List.of(processToQuery));

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildProcessVariableNamesRequest(variableRequestDto, false)
            .withoutAuthentication()
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void getVariableNames() {
    // when
    List<ProcessVariableNameResponseDto> responseList =
        variablesClient.getProcessVariableNames("akey", "aVersion");

    // then
    assertThat(responseList.isEmpty()).isTrue();
  }

  @Test
  public void getVariableNamesWithoutDefinitionVersionDoesNotFail() {
    // given
    ProcessToQueryDto processToQuery = new ProcessToQueryDto();
    processToQuery.setProcessDefinitionKey("akey");

    ProcessVariableNameRequestDto variableRequestDto =
        new ProcessVariableNameRequestDto(List.of(processToQuery));

    // when
    List<ProcessVariableNameResponseDto> responseList =
        variablesClient.getProcessVariableNames(variableRequestDto);

    // then
    assertThat(responseList.isEmpty()).isTrue();
  }

  @Test
  public void getVariableValuesWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildProcessVariableValuesRequest(new ProcessVariableValueRequestDto())
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void getVariableValues() {
    // given
    ProcessVariableValueRequestDto requestDto = new ProcessVariableValueRequestDto();
    requestDto.setProcessDefinitionKey("aKey");
    requestDto.setProcessDefinitionVersion("aVersion");
    requestDto.setName("bla");
    requestDto.setType(BOOLEAN);

    // when
    List responseList = variablesClient.getProcessVariableValues(requestDto);

    // then
    assertThat(responseList.isEmpty()).isTrue();
  }

  @Test
  public void getVariableValuesWithoutDefinitionVersionDoesNotFail() {
    // given
    ProcessVariableValueRequestDto requestDto = new ProcessVariableValueRequestDto();
    requestDto.setProcessDefinitionKey("aKey");
    requestDto.setName("bla");
    requestDto.setType(BOOLEAN);

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildProcessVariableValuesRequest(requestDto)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }
}
