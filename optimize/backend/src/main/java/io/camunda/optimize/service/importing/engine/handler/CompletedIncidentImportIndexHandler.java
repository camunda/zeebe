/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.handler;

import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.importing.TimestampBasedEngineImportIndexHandler;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CompletedIncidentImportIndexHandler extends TimestampBasedEngineImportIndexHandler {

  private static final String COMPLETED_INCIDENT_IMPORT_INDEX_DOC_ID =
      "completedIncidentImportIndex";

  private final EngineContext engineContext;

  public CompletedIncidentImportIndexHandler(final EngineContext engineContext) {
    this.engineContext = engineContext;
  }

  @Override
  public String getEngineAlias() {
    return engineContext.getEngineAlias();
  }

  @Override
  protected String getDatabaseDocID() {
    return COMPLETED_INCIDENT_IMPORT_INDEX_DOC_ID;
  }
}
