/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.writer;

import static io.camunda.optimize.service.db.schema.index.IndexMappingCreatorBuilder.ACTIVITY_EVENT_INDEX;

import io.camunda.optimize.dto.optimize.ImportRequestDto;
import io.camunda.optimize.dto.optimize.RequestType;
import io.camunda.optimize.dto.optimize.query.event.process.CamundaActivityEventDto;
import io.camunda.optimize.service.db.reader.CamundaActivityEventReader;
import io.camunda.optimize.service.db.repository.EventRepository;
import io.camunda.optimize.service.db.repository.IndexRepository;
import io.camunda.optimize.service.db.schema.index.events.CamundaActivityEventIndex;
import io.camunda.optimize.service.util.IdGenerator;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CamundaActivityEventWriter {
  private final IndexRepository indexRepository;
  private final EventRepository eventRepository;
  private final CamundaActivityEventReader camundaActivityEventReader;

  public List<ImportRequestDto> generateImportRequests(
      final List<CamundaActivityEventDto> camundaActivityEvents) {
    final String importItemName = "camunda activity events";
    log.debug("Creating imports for {} [{}].", camundaActivityEvents.size(), importItemName);

    final Set<String> processDefinitionKeysInBatch =
        camundaActivityEvents.stream()
            .map(CamundaActivityEventDto::getProcessDefinitionKey)
            .collect(Collectors.toSet());

    createMissingActivityIndicesForProcessDefinitions(processDefinitionKeysInBatch);

    return camundaActivityEvents.stream()
        .map(entry -> createIndexRequestForActivityEvent(entry, importItemName))
        .toList();
  }

  public void deleteByProcessInstanceIds(
      final String definitionKey, final List<String> processInstanceIds) {
    log.debug(
        "Deleting camunda activity events for [{}] processInstanceIds", processInstanceIds.size());
    eventRepository.deleteByProcessInstanceIds(definitionKey, processInstanceIds);
  }

  private ImportRequestDto createIndexRequestForActivityEvent(
      final CamundaActivityEventDto camundaActivityEventDto, final String importName) {
    return ImportRequestDto.builder()
        .indexName(
            CamundaActivityEventIndex.constructIndexName(
                camundaActivityEventDto.getProcessDefinitionKey()))
        .id(IdGenerator.getNextId())
        .type(RequestType.INDEX)
        .source(camundaActivityEventDto)
        .importName(importName)
        .build();
  }

  private void createMissingActivityIndicesForProcessDefinitions(
      final Set<String> processDefinitionKeys) {
    final Set<String> currentProcessDefinitions =
        camundaActivityEventReader.getIndexSuffixesForCurrentActivityIndices();
    processDefinitionKeys.removeAll(currentProcessDefinitions);
    indexRepository.createMissingIndices(
        ACTIVITY_EVENT_INDEX, Collections.emptySet(), processDefinitionKeys);
  }
}
