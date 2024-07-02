/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.cleanup;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.EXTERNAL_PROCESS_VARIABLE_INDEX_NAME;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableDto;
import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
import io.camunda.optimize.service.util.configuration.cleanup.ExternalVariableCleanupConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class ExternalVariableCleanupIT extends AbstractCleanupIT {

  @Test
  public void externalVariableCleanupWorks() {
    // given
    getExternalVariableCleanupConfiguration().setEnabled(true);
    final List<ExternalProcessVariableRequestDto> variablesToKeep =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();
    final List<ExternalProcessVariableRequestDto> variablesToClean =
        IntStream.range(20, 30)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();

    // freeze time to manipulate ingestion timestamp
    final OffsetDateTime now = dateFreezer().freezeDateAndReturn();
    ingestionClient.ingestVariables(variablesToKeep);
    dateFreezer().dateToFreeze(getEndTimeLessThanGlobalTtl()).freezeDateAndReturn();
    ingestionClient.ingestVariables(variablesToClean);
    dateFreezer().dateToFreeze(now).freezeDateAndReturn();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    assertThat(getAllStoredExternalVariables())
        .extracting(ExternalProcessVariableDto::getVariableId)
        .containsExactlyInAnyOrderElementsOf(
            variablesToKeep.stream()
                .map(ExternalProcessVariableRequestDto::getId)
                .collect(toSet()));
  }

  @Test
  public void externalVariableCleanupCanBeDisabled() {
    // given
    getExternalVariableCleanupConfiguration().setEnabled(false);
    final List<ExternalProcessVariableRequestDto> variables =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();

    // freeze time to manipulate ingestion timestamp
    final OffsetDateTime now = dateFreezer().freezeDateAndReturn();
    dateFreezer().dateToFreeze(getEndTimeLessThanGlobalTtl()).freezeDateAndReturn();
    ingestionClient.ingestVariables(variables);
    dateFreezer().dateToFreeze(now).freezeDateAndReturn();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    assertThat(getAllStoredExternalVariables())
        .extracting(ExternalProcessVariableDto::getVariableId)
        .containsExactlyInAnyOrderElementsOf(
            variables.stream().map(ExternalProcessVariableRequestDto::getId).collect(toSet()));
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
