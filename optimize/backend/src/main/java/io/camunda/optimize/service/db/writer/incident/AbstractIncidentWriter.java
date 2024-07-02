/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.writer.incident;

import io.camunda.optimize.dto.optimize.ImportRequestDto;
import io.camunda.optimize.dto.optimize.persistence.incident.IncidentDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface AbstractIncidentWriter {

  Logger log = LoggerFactory.getLogger(AbstractIncidentWriter.class);

  default List<ImportRequestDto> generateIncidentImports(List<IncidentDto> incidents) {
    final String importItemName = "incidents";
    log.debug("Creating imports for {} [{}].", incidents.size(), importItemName);

    createInstanceIndicesFromIncidentsIfMissing(incidents);

    Map<String, List<IncidentDto>> processInstanceToEvents = new HashMap<>();
    for (IncidentDto e : incidents) {
      processInstanceToEvents.putIfAbsent(e.getProcessInstanceId(), new ArrayList<>());
      processInstanceToEvents.get(e.getProcessInstanceId()).add(e);
    }

    return processInstanceToEvents.entrySet().stream()
        .map(entry -> createImportRequestForIncident(entry, importItemName))
        .collect(Collectors.toList());
  }

  void createInstanceIndicesFromIncidentsIfMissing(final List<IncidentDto> incidents);

  ImportRequestDto createImportRequestForIncident(
      Map.Entry<String, List<IncidentDto>> incidentsByProcessInstance, final String importName);

  String createInlineUpdateScript();
}
