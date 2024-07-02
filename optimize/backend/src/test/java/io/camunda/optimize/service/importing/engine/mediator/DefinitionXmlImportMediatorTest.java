/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.mediator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.optimize.dto.engine.DecisionDefinitionXmlEngineDto;
import io.camunda.optimize.service.importing.engine.fetcher.instance.DecisionDefinitionXmlFetcher;
import io.camunda.optimize.service.importing.engine.handler.DecisionDefinitionXmlImportIndexHandler;
import io.camunda.optimize.service.importing.engine.service.definition.DecisionDefinitionXmlImportService;
import io.camunda.optimize.service.importing.page.IdSetBasedImportPage;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.ConfigurationServiceBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefinitionXmlImportMediatorTest {

  @InjectMocks private DecisionDefinitionXmlEngineImportMediator underTest;

  @Mock private DecisionDefinitionXmlFetcher engineEntityFetcher;

  @Mock private DecisionDefinitionXmlImportIndexHandler importIndexHandler;

  @Mock private DecisionDefinitionXmlImportService importService;

  @Mock private BackoffCalculator idleBackoffCalculator;

  private final ConfigurationService configurationService =
      ConfigurationServiceBuilder.createDefaultConfiguration();

  @BeforeEach
  public void init() {
    this.underTest =
        new DecisionDefinitionXmlEngineImportMediator(
            importIndexHandler,
            engineEntityFetcher,
            importService,
            configurationService,
            idleBackoffCalculator);
  }

  @Test
  public void testImportNextEnginePageWithEmptyIdSet() {
    // given
    IdSetBasedImportPage page = new IdSetBasedImportPage();
    page.setIds(new HashSet<>());
    when(importIndexHandler.getNextPage()).thenReturn(page);

    // when
    final boolean result = underTest.importNextPage(() -> {});

    // then
    assertThat(result).isFalse();
    verify(importIndexHandler, never()).updateIndex(anyInt());
    verify(importService, never()).executeImport(anyList(), any(Runnable.class));
  }

  @Test
  public void testImportNextEnginePageWithNotEmptyIdSet() {
    // given
    IdSetBasedImportPage page = new IdSetBasedImportPage();
    Set<String> testIds = new HashSet<>();
    testIds.add("testID");
    testIds.add("testID2");
    page.setIds(testIds);
    when(importIndexHandler.getNextPage()).thenReturn(page);

    List<DecisionDefinitionXmlEngineDto> resultList = new ArrayList<>();
    resultList.add(new DecisionDefinitionXmlEngineDto());
    when(engineEntityFetcher.fetchXmlsForDefinitions(page)).thenReturn(resultList);

    // when
    final Runnable importCompleteCallback = () -> {};
    final boolean result = underTest.importNextPage(importCompleteCallback);

    // then
    assertThat(result).isTrue();
    verify(importIndexHandler, times(1)).updateIndex(testIds.size());
    verify(importService, times(1)).executeImport(resultList, importCompleteCallback);
  }
}
