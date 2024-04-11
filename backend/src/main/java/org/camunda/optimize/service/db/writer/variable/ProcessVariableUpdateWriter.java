/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.service.db.writer.variable;

import static org.camunda.optimize.service.db.DatabaseConstants.NUMBER_OF_RETRIES_ON_CONFLICT;
import static org.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_MULTI_ALIAS;
import static org.camunda.optimize.service.db.schema.index.IndexMappingCreatorBuilder.PROCESS_INSTANCE_INDEX;
import static org.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.VARIABLES;
import static org.camunda.optimize.service.util.InstanceIndexUtil.getProcessInstanceIndexAliasName;
import static org.camunda.optimize.service.util.VariableHelper.isProcessVariableTypeSupported;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.camunda.optimize.dto.optimize.ImportRequestDto;
import org.camunda.optimize.dto.optimize.ProcessInstanceDto;
import org.camunda.optimize.dto.optimize.RequestType;
import org.camunda.optimize.dto.optimize.datasource.EngineDataSourceDto;
import org.camunda.optimize.dto.optimize.query.variable.ProcessVariableDto;
import org.camunda.optimize.dto.optimize.query.variable.SimpleProcessVariableDto;
import org.camunda.optimize.service.db.repository.IndexRepository;
import org.camunda.optimize.service.db.repository.VariableRepository;
import org.camunda.optimize.service.db.repository.script.ProcessVariableScriptFactory;
import org.camunda.optimize.service.db.schema.ScriptData;
import org.camunda.optimize.service.db.writer.DatabaseWriterUtil;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class ProcessVariableUpdateWriter {
  private static final String VARIABLE_UPDATES_FROM_ENGINE = "variableUpdatesFromEngine";

  private final ObjectMapper objectMapper;
  private final IndexRepository indexRepository;
  private final VariableRepository variableRepository;

  public List<ImportRequestDto> generateVariableUpdateImports(
      final List<ProcessVariableDto> variables) {
    final String importItemName = "variables";
    log.debug("Creating imports for {} [{}].", variables.size(), importItemName);

    final Set<String> keys =
        variables.stream()
            .map(ProcessVariableDto::getProcessDefinitionKey)
            .collect(Collectors.toSet());

    indexRepository.createMissingIndices(
        PROCESS_INSTANCE_INDEX, Set.of(PROCESS_INSTANCE_MULTI_ALIAS), keys);

    final Map<String, List<ProcessVariableDto>> processInstanceIdToVariables =
        groupVariablesByProcessInstanceIds(variables);

    return processInstanceIdToVariables.entrySet().stream()
        .map(entry -> createUpdateRequestForProcessInstanceVariables(entry, importItemName))
        .collect(Collectors.toList());
  }

  public void deleteVariableDataByProcessInstanceIds(
      final String processDefinitionKey, final List<String> processInstanceIds) {
    log.debug(
        "Deleting variable data on [{}] process instance documents with bulk request.",
        processInstanceIds.size());
    variableRepository.deleteVariableDataByProcessInstanceIds(
        processDefinitionKey, processInstanceIds);
  }

  private ImportRequestDto createUpdateRequestForProcessInstanceVariables(
      final Map.Entry<String, List<ProcessVariableDto>> processInstanceIdToVariables,
      final String importItemName) {
    final List<ProcessVariableDto> variablesWithAllInformation =
        processInstanceIdToVariables.getValue();
    final String processInstanceId = processInstanceIdToVariables.getKey();
    final String processDefinitionKey =
        variablesWithAllInformation.get(0).getProcessDefinitionKey();

    final List<SimpleProcessVariableDto> variables =
        mapToSimpleVariables(variablesWithAllInformation);
    final Map<String, Object> params = buildParameters(variables);

    final ScriptData updateScriptData =
        DatabaseWriterUtil.createScriptData(createInlineUpdateScript(), params, objectMapper);

    if (variablesWithAllInformation.isEmpty()) {
      // all is lost, no variables to persist, should have crashed before.
      return null;
    }
    final ProcessVariableDto firstVariable = variablesWithAllInformation.get(0);
    final ProcessInstanceDto processInstanceDto =
        getNewProcessInstanceRecord(
            processInstanceId,
            firstVariable.getEngineAlias(),
            firstVariable.getTenantId(),
            variables);

    return ImportRequestDto.builder()
        .indexName(getProcessInstanceIndexAliasName(processDefinitionKey))
        .importName(importItemName)
        .type(RequestType.UPDATE)
        .id(processInstanceId)
        .scriptData(updateScriptData)
        .source(processInstanceDto)
        .retryNumberOnConflict(NUMBER_OF_RETRIES_ON_CONFLICT)
        .build();
  }

  private Map<String, List<ProcessVariableDto>> groupVariablesByProcessInstanceIds(
      final List<ProcessVariableDto> variableUpdates) {
    final Map<String, List<ProcessVariableDto>> processInstanceIdToVariables = new HashMap<>();
    for (final ProcessVariableDto variable : variableUpdates) {
      if (isVariableFromCaseDefinition(variable)
          || !isProcessVariableTypeSupported(variable.getType())) {
        log.warn(
            "Variable [{}] is either a case definition variable or the type [{}] is not supported!",
            variable,
            variable.getType());
        continue;
      }
      processInstanceIdToVariables.putIfAbsent(variable.getProcessInstanceId(), new ArrayList<>());
      processInstanceIdToVariables.get(variable.getProcessInstanceId()).add(variable);
    }
    return processInstanceIdToVariables;
  }

  private List<SimpleProcessVariableDto> mapToSimpleVariables(
      final List<ProcessVariableDto> variablesWithAllInformation) {
    return variablesWithAllInformation.stream()
        .map(
            var ->
                new SimpleProcessVariableDto(
                    var.getId(), var.getName(), var.getType(), var.getValue(), var.getVersion()))
        .map(
            variable -> {
              if (variable.getValue().stream().allMatch(Objects::isNull)) {
                variable.setValue(Collections.emptyList());
              }
              return variable;
            })
        .collect(Collectors.toList());
  }

  private Map<String, Object> buildParameters(final List<SimpleProcessVariableDto> variables) {
    final Map<String, Object> params = new HashMap<>();
    params.put(VARIABLE_UPDATES_FROM_ENGINE, variables);
    return params;
  }

  private String createInlineUpdateScript() {
    final StringBuilder builder = new StringBuilder();
    final Map<String, String> substitutions = new HashMap<>();
    substitutions.put("variables", VARIABLES);
    substitutions.put("variableUpdatesFromEngine", VARIABLE_UPDATES_FROM_ENGINE);
    final StringSubstitutor sub = new StringSubstitutor(substitutions);
    final String variableScript = ProcessVariableScriptFactory.createInlineUpdateScript();
    final String resolvedVariableScript = sub.replace(variableScript);
    builder.append(resolvedVariableScript);
    return builder.toString();
  }

  private ProcessInstanceDto getNewProcessInstanceRecord(
      final String processInstanceId,
      final String engineAlias,
      final String tenantId,
      final List<SimpleProcessVariableDto> variables) {
    final ProcessInstanceDto procInst =
        ProcessInstanceDto.builder()
            .processInstanceId(processInstanceId)
            .dataSource(new EngineDataSourceDto(engineAlias))
            .tenantId(tenantId)
            .build();
    procInst.getVariables().addAll(variables);

    return procInst;
  }

  private boolean isVariableFromCaseDefinition(final ProcessVariableDto variable) {
    // if the variable instance is not related to a process instance we assume it's originating from
    // a case definition
    return variable.getProcessInstanceId() == null;
  }
}
