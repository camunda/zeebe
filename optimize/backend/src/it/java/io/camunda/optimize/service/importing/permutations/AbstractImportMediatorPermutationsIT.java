/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.permutations;

import static io.camunda.optimize.service.util.configuration.ConfigurationServiceConstants.PLATFORM_PROFILE;
import static io.camunda.optimize.service.util.configuration.EnvironmentPropertiesConstants.INTEGRATION_TESTS;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.importing.ImportMediator;
import io.camunda.optimize.service.importing.engine.EngineImportScheduler;
import io.camunda.optimize.test.it.extension.DatabaseIntegrationTestExtension;
import io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension;
import io.camunda.optimize.test.it.extension.EngineIntegrationExtension;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {INTEGRATION_TESTS + "=true"})
@Configuration
@Tag("import")
@ActiveProfiles(PLATFORM_PROFILE)
public abstract class AbstractImportMediatorPermutationsIT {
  protected static final String TEST_PROCESS = "process";
  protected static final String CANDIDATE_GROUP = "candidateGroup";

  // static extension setup with disabled cleanup to reduce initialization/cleanup overhead
  @RegisterExtension
  @Order(1)
  public static DatabaseIntegrationTestExtension databaseIntegrationTestExtension =
      new DatabaseIntegrationTestExtension(false);

  @RegisterExtension
  @Order(2)
  public static EngineIntegrationExtension engineIntegrationExtension =
      new EngineIntegrationExtension(false);

  @RegisterExtension
  @Order(3)
  public static EmbeddedOptimizeExtension embeddedOptimizeExtension =
      new EmbeddedOptimizeExtension(true);

  @BeforeAll
  static void beforeAll() {
    engineIntegrationExtension.cleanEngine();
    databaseIntegrationTestExtension.deleteAllOptimizeData();
    databaseIntegrationTestExtension.deleteAllProcessInstanceIndices();
    databaseIntegrationTestExtension.deleteAllDecisionInstanceIndices();
    embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
  }

  @AfterEach
  public void after() {
    embeddedOptimizeExtension.resetImportStartIndexes();
    embeddedOptimizeExtension.resetInstanceDataWriters();
    databaseIntegrationTestExtension.deleteAllOptimizeData();
  }

  @SneakyThrows
  protected void performOrderedImport(final List<Class<? extends ImportMediator>> mediatorOrder) {
    for (EngineImportScheduler scheduler :
        embeddedOptimizeExtension.getImportSchedulerManager().getEngineImportSchedulers()) {
      final List<ImportMediator> sortedMediators =
          scheduler.getImportMediators().stream()
              .filter(
                  engineImportMediator -> mediatorOrder.contains(engineImportMediator.getClass()))
              .sorted(Comparator.comparingInt(o -> mediatorOrder.indexOf(o.getClass())))
              .toList();

      for (ImportMediator sortedMediator : sortedMediators) {
        // run and wait for each mediator to finish the import run to force a certain execution
        // order
        sortedMediator.runImport().get(30, TimeUnit.SECONDS);
      }
    }
  }

  protected static ProcessInstanceEngineDto deployAndStartUserTaskProcess() {
    return engineIntegrationExtension.deployAndStartProcessWithVariables(
        getSingleUserTaskDiagram(TEST_PROCESS), ImmutableMap.of("var1", 1, "var2", "2"));
  }

  protected static Stream<List<Class<? extends ImportMediator>>> getMediatorPermutationsStream(
      final List<Class<? extends ImportMediator>> mediatorClasses) {
    return Collections2.permutations(mediatorClasses).stream();
  }
}
