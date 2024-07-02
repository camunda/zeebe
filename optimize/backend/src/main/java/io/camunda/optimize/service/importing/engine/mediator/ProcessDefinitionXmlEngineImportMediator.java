/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.mediator;

import io.camunda.optimize.dto.engine.ProcessDefinitionXmlEngineDto;
import io.camunda.optimize.service.importing.engine.fetcher.instance.ProcessDefinitionXmlFetcher;
import io.camunda.optimize.service.importing.engine.handler.ProcessDefinitionXmlImportIndexHandler;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionXmlImportService;
import io.camunda.optimize.service.importing.page.IdSetBasedImportPage;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProcessDefinitionXmlEngineImportMediator
    extends DefinitionXmlImportMediator<
        ProcessDefinitionXmlImportIndexHandler, ProcessDefinitionXmlEngineDto> {

  private final ProcessDefinitionXmlFetcher engineEntityFetcher;

  public ProcessDefinitionXmlEngineImportMediator(
      final ProcessDefinitionXmlImportIndexHandler importIndexHandler,
      final ProcessDefinitionXmlFetcher engineEntityFetcher,
      final ProcessDefinitionXmlImportService importService,
      final ConfigurationService configurationService,
      final BackoffCalculator idleBackoffCalculator) {
    super(configurationService, idleBackoffCalculator, importIndexHandler, importService);
    this.engineEntityFetcher = engineEntityFetcher;
  }

  @Override
  protected List<ProcessDefinitionXmlEngineDto> getEntities(final IdSetBasedImportPage page) {
    return engineEntityFetcher.fetchXmlsForDefinitions(page);
  }

  @Override
  public MediatorRank getRank() {
    return MediatorRank.DEFINITION_XML;
  }
}
