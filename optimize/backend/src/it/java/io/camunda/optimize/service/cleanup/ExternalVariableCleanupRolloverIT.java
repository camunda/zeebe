/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.cleanup;

import static io.camunda.optimize.service.db.DatabaseConstants.EXTERNAL_PROCESS_VARIABLE_INDEX_NAME;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableDto;
import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
import io.camunda.optimize.service.db.es.schema.index.ExternalProcessVariableIndexES;
import io.camunda.optimize.service.util.configuration.cleanup.ExternalVariableCleanupConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExternalVariableCleanupRolloverIT extends AbstractCleanupIT {

  @BeforeEach
  @AfterEach
  public void cleanUpExternalVariableIndices() {
    databaseIntegrationTestExtension.deleteAllExternalVariableIndices();
    embeddedOptimizeExtension
        .getDatabaseSchemaManager()
        .createOrUpdateOptimizeIndex(
            embeddedOptimizeExtension.getOptimizeDatabaseClient(),
            new ExternalProcessVariableIndexES());
  }

  @Test
  public void cleanupWorksAfterRollover() {
    // given
    getExternalVariableCleanupConfiguration().setEnabled(true);
    final List<ExternalProcessVariableRequestDto> varsToCleanIndex1 =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();

    // freeze time to manipulate ingestion timestamp
    final OffsetDateTime now = dateFreezer().freezeDateAndReturn();
    dateFreezer().dateToFreeze(getEndTimeLessThanGlobalTtl()).freezeDateAndReturn();
    ingestionClient.ingestVariables(varsToCleanIndex1);

    // trigger rollover
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    embeddedOptimizeExtension
        .getConfigurationService()
        .getVariableIndexRolloverConfiguration()
        .setMaxIndexSizeGB(0);
    embeddedOptimizeExtension.getExternalProcessVariableIndexRolloverService().triggerRollover();

    final List<ExternalProcessVariableRequestDto> varsToCleanIndex2 =
        IntStream.range(20, 30)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();
    final List<ExternalProcessVariableRequestDto> varsToKeepIndex2 =
        IntStream.range(40, 50)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();

    // freeze time to manipulate ingestion timestamp
    ingestionClient.ingestVariables(varsToCleanIndex2);
    dateFreezer().dateToFreeze(now).freezeDateAndReturn();
    ingestionClient.ingestVariables(varsToKeepIndex2);
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    assertThat(getAllStoredExternalVariables())
        .extracting(ExternalProcessVariableDto::getVariableId)
        .containsExactlyInAnyOrderElementsOf(
            varsToKeepIndex2.stream()
                .map(ExternalProcessVariableRequestDto::getId)
                .collect(toList()));
  }

  private ExternalVariableCleanupConfiguration getExternalVariableCleanupConfiguration() {
    return embeddedOptimizeExtension
        .getConfigurationService()
        .getCleanupServiceConfiguration()
        .getExternalVariableCleanupConfiguration();
  }

  private List<ExternalProcessVariableDto> getAllStoredExternalVariables() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        EXTERNAL_PROCESS_VARIABLE_INDEX_NAME, ExternalProcessVariableDto.class);
  }
}
