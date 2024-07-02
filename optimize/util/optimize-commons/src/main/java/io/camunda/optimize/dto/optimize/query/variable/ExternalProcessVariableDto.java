/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.variable;

import static io.camunda.optimize.service.db.schema.index.ExternalProcessVariableIndex.SERIALIZATION_DATA_FORMAT;

import io.camunda.optimize.dto.optimize.OptimizeDto;
import io.camunda.optimize.plugin.importing.variable.PluginVariableDto;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

@Data
@Accessors(chain = true)
@FieldNameConstants
public class ExternalProcessVariableDto implements OptimizeDto {
  private String variableId;
  private String variableName;
  private String variableValue;
  private VariableType variableType;
  private Long ingestionTimestamp;
  private String processInstanceId;
  private String processDefinitionKey;
  private String serializationDataFormat; // optional, used for object variables

  public static List<PluginVariableDto> toPluginVariableDtos(
      final List<ExternalProcessVariableDto> variableDtos) {
    final Map<String, Object> valueInfo = new HashMap<>();
    return variableDtos.stream()
        .map(
            varDto -> {
              valueInfo.put(SERIALIZATION_DATA_FORMAT, varDto.getSerializationDataFormat());
              return new PluginVariableDto()
                  .setTimestamp(
                      OffsetDateTime.ofInstant(
                          Instant.ofEpochMilli(varDto.getIngestionTimestamp()),
                          ZoneId.systemDefault()))
                  .setId(varDto.getVariableId())
                  .setName(varDto.getVariableName())
                  .setValue(varDto.getVariableValue())
                  .setType(varDto.getVariableType().getId())
                  .setValueInfo(valueInfo)
                  .setProcessInstanceId(varDto.getProcessInstanceId())
                  .setProcessDefinitionKey(varDto.getProcessDefinitionKey());
            })
        .toList();
  }
}
