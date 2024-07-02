/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.persistence.incident;

import static io.camunda.optimize.service.util.importing.EngineConstants.FAILED_EXTERNAL_TASK_INCIDENT_TYPE;
import static io.camunda.optimize.service.util.importing.EngineConstants.FAILED_JOB_INCIDENT_TYPE;

import com.fasterxml.jackson.annotation.JsonValue;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentType {

  /*
   Those are just the predefined incident types that are raised by the engine
   out of the box. However, it's possible to create custom incident types.
   For more, see:
   https://docs.camunda.org/manual/latest/user-guide/process-engine/incidents/#incident-types
  */
  private static final IncidentType FAILED_JOB = new IncidentType(FAILED_JOB_INCIDENT_TYPE);
  public static final IncidentType FAILED_EXTERNAL_TASK =
      new IncidentType(FAILED_EXTERNAL_TASK_INCIDENT_TYPE);

  private final String id;

  @JsonValue
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return getId();
  }

  public static IncidentType valueOfId(final String incidentTypeId) {
    if (incidentTypeId == null) {
      throw new OptimizeRuntimeException("Incident type not allowed to be null!");
    }
    if (!FAILED_JOB.getId().equals(incidentTypeId)
        && !FAILED_EXTERNAL_TASK.getId().equals(incidentTypeId)) {
      log.debug("Importing custom incident type [{}]", incidentTypeId);
    }
    return new IncidentType(incidentTypeId);
  }
}
