/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.importing;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.util.BpmnModels.getExternalTaskProcess;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import java.util.List;
// import lombok.SneakyThrows;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class ImportIndexIT extends AbstractImportIT {
//
//   @Test
//   public void importIndexIsZeroIfNothingIsImportedYet() {
//     // when
//     List<Long> indexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // then
//     for (Long index : indexes) {
//       assertThat(index).isZero();
//     }
//   }
//
//   @Test
//   public void indexLastTimestampIsEqualEvenAfterReset() throws InterruptedException {
//     // given
//     final int currentTimeBackOff = 1000;
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .setCurrentTimeBackoffMilliseconds(currentTimeBackOff);
//     deployAndStartSimpleServiceTaskProcess();
//     deployAndStartSimpleServiceTaskProcess();
//
//     // sleep in order to avoid the timestamp import backoff window that modifies the
// latestTimestamp
//     // stored
//     Thread.sleep(currentTimeBackOff);
//
//     importAllEngineEntitiesFromScratch();
//     List<Long> firstRoundIndexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // then
//     embeddedOptimizeExtension.resetImportStartIndexes();
//     importAllEngineEntitiesFromScratch();
//     List<Long> secondsRoundIndexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // then
//     for (int i = 0; i < firstRoundIndexes.size(); i++) {
//       assertThat(firstRoundIndexes.get(i)).isEqualTo(secondsRoundIndexes.get(i));
//     }
//   }
//
//   @Test
//   @SneakyThrows
//   public void latestImportIndexAfterRestartOfOptimize() {
//     // given
//     deployAllPossibleEngineData();
//
//     importAllEngineEntitiesFromScratch();
//     embeddedOptimizeExtension.storeImportIndexesToElasticsearch();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     startAndUseNewOptimizeInstance();
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertThat(embeddedOptimizeExtension.getImportIndexes())
//         .allSatisfy(index -> assertThat(index).isPositive());
//   }
//
//   @Test
//   public void indexAfterRestartOfOptimizeHasCorrectProcessDefinitionsToImport() {
//     // given
//     deployAndStartSimpleServiceTaskProcess();
//     deployAndStartSimpleServiceTaskProcess();
//     deployAndStartSimpleServiceTaskProcess();
//     importAllEngineEntitiesFromScratch();
//     embeddedOptimizeExtension.storeImportIndexesToElasticsearch();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//     List<Long> firstRoundIndexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//     List<Long> secondsRoundIndexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // then
//     for (int i = 0; i < firstRoundIndexes.size(); i++) {
//       assertThat(firstRoundIndexes.get(i)).isEqualTo(secondsRoundIndexes.get(i));
//     }
//   }
//
//   @Test
//   public void afterRestartOfOptimizeAlsoNewDataIsImported() {
//     // given
//     deployAndStartSimpleServiceTaskProcess();
//     importAllEngineEntitiesFromScratch();
//     List<Long> firstRoundIndexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // and
//     deployAndStartSimpleServiceTaskProcess();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//     List<Long> secondsRoundIndexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // then
//     for (int i = 0; i < firstRoundIndexes.size(); i++) {
//       assertThat(firstRoundIndexes.get(i)).isLessThanOrEqualTo(secondsRoundIndexes.get(i));
//     }
//   }
//
//   @Test
//   public void itIsPossibleToResetTheImportIndex() {
//     // given
//     deployAndStartSimpleServiceTaskProcess();
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.resetImportStartIndexes();
//     embeddedOptimizeExtension.storeImportIndexesToElasticsearch();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     List<Long> indexes = embeddedOptimizeExtension.getImportIndexes();
//
//     // then
//     for (Long index : indexes) {
//       assertThat(index).isZero();
//     }
//   }
//
//   private void deployAllPossibleEngineData() {
//     deployAndStartUserTaskProcess();
//     // we need finished ones
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     // as well as running & suspended ones
//     final ProcessInstanceEngineDto processInstanceToSuspend = deployAndStartUserTaskProcess();
//
// engineIntegrationExtension.suspendProcessInstanceByInstanceId(processInstanceToSuspend.getId());
//     deployAndStartSimpleServiceTaskProcess();
//     engineIntegrationExtension.deployAndStartDecisionDefinition();
//     engineIntegrationExtension.createTenant("id", "name");
//
//     // create incident data
//     ProcessInstanceEngineDto processInstanceEngineDto =
//         engineIntegrationExtension.deployAndStartProcess(getExternalTaskProcess());
//     incidentClient.createOpenIncidentForInstancesWithBusinessKey(
//         processInstanceEngineDto.getBusinessKey());
//     incidentClient.resolveOpenIncidents(processInstanceEngineDto.getId());
//     processInstanceEngineDto =
//         engineIntegrationExtension.deployAndStartProcess(getExternalTaskProcess());
//     incidentClient.createOpenIncidentForInstancesWithBusinessKey(
//         processInstanceEngineDto.getBusinessKey());
//   }
// }
