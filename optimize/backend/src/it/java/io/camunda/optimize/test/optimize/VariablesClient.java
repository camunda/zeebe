/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.optimize;

import static io.camunda.optimize.dto.optimize.query.variable.VariableType.STRING;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import io.camunda.optimize.OptimizeRequestExecutor;
import io.camunda.optimize.dto.engine.definition.DecisionDefinitionEngineDto;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.variable.DecisionVariableNameRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.DecisionVariableNameResponseDto;
import io.camunda.optimize.dto.optimize.query.variable.DecisionVariableValueRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessToQueryDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameResponseDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableReportValuesRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableValueRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.rest.optimize.dto.VariableDto;
import io.camunda.optimize.service.util.importing.EngineConstants;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.SneakyThrows;

public class VariablesClient {
  private final Supplier<OptimizeRequestExecutor> requestExecutorSupplier;
  private final ObjectMapper objectMapper;

  public VariablesClient(final Supplier<OptimizeRequestExecutor> requestExecutorSupplier) {
    this.requestExecutorSupplier = requestExecutorSupplier;
    objectMapper =
        new ObjectMapper()
            .setDateFormat(new SimpleDateFormat(OPTIMIZE_DATE_FORMAT))
            .enable(SerializationFeature.INDENT_OUTPUT);
  }

  public List<ProcessVariableNameResponseDto> getProcessVariableNames(
      ProcessDefinitionEngineDto processDefinition) {
    ProcessToQueryDto processToQuery = new ProcessToQueryDto();
    processToQuery.setProcessDefinitionKey(processDefinition.getKey());
    processToQuery.setProcessDefinitionVersions(
        ImmutableList.of(processDefinition.getVersionAsString()));

    List<ProcessToQueryDto> processesToQuery = List.of(processToQuery);
    ProcessVariableNameRequestDto variableRequestDto =
        new ProcessVariableNameRequestDto(processesToQuery);

    return getProcessVariableNames(variableRequestDto);
  }

  public List<ProcessVariableNameResponseDto> getProcessVariableNames(
      ProcessVariableNameRequestDto variableRequestDtos) {
    return getRequestExecutor()
        .buildProcessVariableNamesRequest(variableRequestDtos)
        .executeAndReturnList(
            ProcessVariableNameResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public List<ProcessVariableNameResponseDto> getProcessVariableNamesForReportIds(
      final List<String> reportIds) {
    return getRequestExecutor()
        .buildProcessVariableNamesForReportsRequest(reportIds)
        .executeAndReturnList(
            ProcessVariableNameResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public List<String> getProcessVariableValuesForReports(
      final ProcessVariableReportValuesRequestDto dto) {
    return getRequestExecutor()
        .buildProcessVariableValuesForReportsRequest(dto)
        .executeAndReturnList(String.class, Response.Status.OK.getStatusCode());
  }

  public List<ProcessVariableNameResponseDto> getProcessVariableNames(
      String key, List<String> versions) {
    ProcessToQueryDto processToQuery = new ProcessToQueryDto();
    processToQuery.setProcessDefinitionKey(key);
    processToQuery.setProcessDefinitionVersions(versions);

    List<ProcessToQueryDto> processesToQuery = List.of(processToQuery);
    ProcessVariableNameRequestDto variableRequestDto =
        new ProcessVariableNameRequestDto(processesToQuery);

    return getProcessVariableNames(variableRequestDto);
  }

  public List<ProcessVariableNameResponseDto> getProcessVariableNames(
      final String key, final String version) {
    return getProcessVariableNames(key, ImmutableList.of(version));
  }

  public List<String> getProcessVariableValues(final ProcessVariableValueRequestDto requestDto) {
    return getRequestExecutor()
        .buildProcessVariableValuesRequest(requestDto)
        .executeAndReturnList(String.class, Response.Status.OK.getStatusCode());
  }

  public List<String> getProcessVariableValues(
      final ProcessDefinitionEngineDto processDefinition, final String variableName) {
    return getProcessVariableValues(null, processDefinition, variableName, STRING);
  }

  public List<String> getProcessVariableValues(
      final String processInstanceId,
      final ProcessDefinitionEngineDto processDefinition,
      final String variableName) {
    return getProcessVariableValues(processInstanceId, processDefinition, variableName, STRING);
  }

  public List<String> getProcessVariableValues(
      final ProcessDefinitionEngineDto processDefinition,
      final String variableName,
      final VariableType variableType) {
    return getProcessVariableValues(null, processDefinition, variableName, variableType);
  }

  public List<String> getProcessVariableValues(
      final String processInstanceId,
      final ProcessDefinitionEngineDto processDefinition,
      final String variableName,
      final VariableType variableType) {
    ProcessVariableValueRequestDto requestDto = new ProcessVariableValueRequestDto();
    requestDto.setProcessInstanceId(processInstanceId);
    requestDto.setProcessDefinitionKey(processDefinition.getKey());
    requestDto.setProcessDefinitionVersion(processDefinition.getVersionAsString());
    requestDto.setName(variableName);
    requestDto.setType(variableType);
    return getProcessVariableValues(requestDto);
  }

  public VariableDto createObjectVariableDto(
      final boolean isNativeJsonVar, final Map<String, Object> variable) {
    if (isNativeJsonVar) {
      return createNativeJsonVariableDto(variable);
    } else {
      return createMapJsonObjectVariableDto(variable);
    }
  }

  public VariableDto createObjectVariableDto(
      final boolean isNativeJsonVar, final List<Object> variable) {
    if (isNativeJsonVar) {
      return createNativeJsonVariableDto(variable);
    } else {
      return createListJsonObjectVariableDto(variable);
    }
  }

  @SneakyThrows
  public VariableDto createMapJsonObjectVariableDto(final Map<String, Object> variable) {
    return createJsonObjectVariableDto(
        objectMapper.writeValueAsString(variable), "java.util.HashMap");
  }

  @SneakyThrows
  public VariableDto createListJsonObjectVariableDto(final List<Object> variable) {
    return createJsonObjectVariableDto(
        objectMapper.writeValueAsString(variable), "java.util.ArrayList");
  }

  @SneakyThrows
  public VariableDto createJsonObjectVariableDto(final String value, final String objectTypeName) {
    VariableDto objectVariableDto = new VariableDto();
    objectVariableDto.setType(EngineConstants.VARIABLE_TYPE_OBJECT);
    objectVariableDto.setValue(value);
    VariableDto.ValueInfo info = new VariableDto.ValueInfo();
    info.setObjectTypeName(objectTypeName);
    info.setSerializationDataFormat(MediaType.APPLICATION_JSON);
    objectVariableDto.setValueInfo(info);
    return objectVariableDto;
  }

  @SneakyThrows
  public VariableDto createNativeJsonVariableDto(final Map<String, Object> variable) {
    return createNativeJsonVariableDto(objectMapper.writeValueAsString(variable));
  }

  @SneakyThrows
  public VariableDto createNativeJsonVariableDto(final List<Object> variable) {
    return createNativeJsonVariableDto(objectMapper.writeValueAsString(variable));
  }

  @SneakyThrows
  public VariableDto createNativeJsonVariableDto(final String value) {
    VariableDto nativeJsonVariableDto = new VariableDto();
    nativeJsonVariableDto.setType(EngineConstants.VARIABLE_TYPE_JSON);
    nativeJsonVariableDto.setValue(value);
    return nativeJsonVariableDto;
  }

  public List<String> getDecisionInputVariableValues(
      final DecisionVariableValueRequestDto variableValueRequestDto) {
    return getRequestExecutor()
        .buildDecisionInputVariableValuesRequest(variableValueRequestDto)
        .executeAndReturnList(String.class, Response.Status.OK.getStatusCode());
  }

  public List<String> getDecisionInputVariableValues(
      final String decisionDefinitionKey,
      final String decisionDefinitionVersion,
      final String variableId,
      final VariableType variableType) {

    return getDecisionInputVariableValues(
        decisionDefinitionKey,
        decisionDefinitionVersion,
        variableId,
        variableType,
        null,
        Integer.MAX_VALUE,
        0);
  }

  public List<String> getDecisionInputVariableValues(
      final DecisionDefinitionEngineDto decisionDefinitionEngineDto,
      final String variableId,
      final VariableType variableType) {
    return getDecisionInputVariableValues(
        decisionDefinitionEngineDto, variableId, variableType, null);
  }

  public List<String> getDecisionInputVariableValues(
      final DecisionDefinitionEngineDto decisionDefinitionEngineDto,
      final String variableId,
      final VariableType variableType,
      final String valueFilter) {
    return getDecisionInputVariableValues(
        decisionDefinitionEngineDto, variableId, variableType, valueFilter, Integer.MAX_VALUE, 0);
  }

  public List<String> getDecisionInputVariableValues(
      final DecisionDefinitionEngineDto decisionDefinitionEngineDto,
      final String variableId,
      final VariableType variableType,
      final String valueFilter,
      final Integer numResults,
      final Integer offset) {
    return getDecisionInputVariableValues(
        decisionDefinitionEngineDto.getKey(),
        String.valueOf(decisionDefinitionEngineDto.getVersion()),
        variableId,
        variableType,
        valueFilter,
        numResults,
        offset);
  }

  public List<String> getDecisionInputVariableValues(
      final String decisionDefinitionKey,
      final String decisionDefinitionVersion,
      final String variableId,
      final VariableType variableType,
      final String valueFilter,
      final Integer numResults,
      final Integer offset) {
    final DecisionVariableValueRequestDto queryParams =
        createDecisionVariableRequest(
            decisionDefinitionKey,
            decisionDefinitionVersion,
            variableId,
            variableType,
            valueFilter,
            numResults,
            offset);
    return getDecisionInputVariableValues(queryParams);
  }

  public DecisionVariableValueRequestDto createDecisionVariableRequest(
      final String decisionDefinitionKey,
      final String decisionDefinitionVersion,
      final String variableId,
      final VariableType variableType,
      final String valueFilter,
      final Integer numResults,
      final Integer offset) {
    DecisionVariableValueRequestDto requestDto = new DecisionVariableValueRequestDto();
    requestDto.setDecisionDefinitionKey(decisionDefinitionKey);
    requestDto.setDecisionDefinitionVersion(decisionDefinitionVersion);
    requestDto.setVariableId(variableId);
    requestDto.setVariableType(variableType);
    requestDto.setValueFilter(valueFilter);
    requestDto.setResultOffset(offset);
    requestDto.setNumResults(numResults);
    return requestDto;
  }

  public List<String> getDecisionOutputVariableValues(
      final DecisionDefinitionEngineDto decisionDefinitionEngineDto,
      final String variableId,
      final VariableType variableType) {
    return getDecisionOutputVariableValues(
        decisionDefinitionEngineDto, variableId, variableType, null);
  }

  public List<String> getDecisionOutputVariableValues(
      final DecisionDefinitionEngineDto decisionDefinitionEngineDto,
      final String variableId,
      final VariableType variableType,
      final String valueFilter) {
    DecisionVariableValueRequestDto variableRequest =
        createDecisionVariableRequest(
            decisionDefinitionEngineDto.getKey(),
            decisionDefinitionEngineDto.getVersionAsString(),
            variableId,
            variableType,
            valueFilter,
            Integer.MAX_VALUE,
            0);
    return getRequestExecutor()
        .buildDecisionOutputVariableValuesRequest(variableRequest)
        .executeAndReturnList(String.class, Response.Status.OK.getStatusCode());
  }

  public List<DecisionVariableNameResponseDto> getDecisionInputVariableNames(
      final DecisionVariableNameRequestDto variableRequestDto) {
    return getDecisionInputVariableNames(Collections.singletonList(variableRequestDto));
  }

  public List<DecisionVariableNameResponseDto> getDecisionInputVariableNames(
      final List<DecisionVariableNameRequestDto> variableRequestDtos) {
    return getRequestExecutor()
        .buildDecisionInputVariableNamesRequest(variableRequestDtos, true)
        .executeAndReturnList(
            DecisionVariableNameResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public List<DecisionVariableNameResponseDto> getDecisionOutputVariableNames(
      final DecisionVariableNameRequestDto variableRequestDto) {
    return getDecisionOutputVariableNames(Collections.singletonList(variableRequestDto));
  }

  public List<DecisionVariableNameResponseDto> getDecisionOutputVariableNames(
      final List<DecisionVariableNameRequestDto> variableRequestDtos) {
    return getRequestExecutor()
        .buildDecisionOutputVariableNamesRequest(variableRequestDtos)
        .executeAndReturnList(
            DecisionVariableNameResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public OptimizeRequestExecutor getRequestExecutor() {
    return requestExecutorSupplier.get();
  }
}
