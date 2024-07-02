/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.test.performance;
// 
// import static io.camunda.optimize.data.generation.DataGenerationMain.getDefaultDefinitionsOfClass;
// import static io.camunda.optimize.data.generation.DataGenerationMain.parseDefinitions;
// 
// import io.camunda.optimize.data.generation.DataGenerationExecutor;
// import io.camunda.optimize.data.generation.generators.dto.DataGenerationInformation;
// import io.camunda.optimize.data.generation.generators.impl.decision.DecisionDataGenerator;
// import io.camunda.optimize.data.generation.generators.impl.process.ProcessDataGenerator;
// import io.camunda.optimize.test.util.PropertyUtil;
// import java.time.OffsetDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.Properties;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.Future;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.TimeUnit;
// import org.junit.jupiter.api.Test;
// 
// public class ImportPerformanceLiveLoadTest extends AbstractImportTest {
// 
//   @Override
//   Properties getProperties() {
//     return PropertyUtil.loadProperties("live-import-test.properties");
//   }
// 
//   @Test
//   public void importWhileGeneratingDataTestPlusAlreadyExistingData() throws Exception {
//     int totalInstanceCountPerGenerationBatch = 5_000;
//     // GIVEN I have data in the engine before optimize starts
//     final Future<Long> dataGenerationTask =
//         startDataGeneration(totalInstanceCountPerGenerationBatch);
//     waitForDataGenerationTaskToComplete(dataGenerationTask);
// 
//     // AND I start optimize & schedule imports
//     logger.info("Starting import of engine data to Optimize...");
//     embeddedOptimizeExtension.startContinuousImportScheduling();
//     ScheduledExecutorService progressReporterExecutorService = reportImportProgress();
// 
//     // WHEN I start another data generation and wait for it to finish
//     final Future<Long> dataGenerationTask1 =
//         startDataGeneration(totalInstanceCountPerGenerationBatch);
//     waitForDataGenerationTaskToComplete(dataGenerationTask1);
// 
//     // wait for data import to finish
//     embeddedOptimizeExtension.ensureImportSchedulerIsIdle(maxImportDurationInMin * 60);
// 
//     // AND wait for the double max backoff period to pass to ensure any backed off mediator runs at
//     // least once more
//     Thread.sleep(2 * configurationService.getMaximumBackoff() * 1000L);
//     embeddedOptimizeExtension.ensureImportSchedulerIsIdle(maxImportDurationInMin * 60);
// 
//     progressReporterExecutorService.shutdown();
// 
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//     logStats();
// 
//     // THEN all data from the engine should be in elasticsearch
//     assertThatEngineAndElasticDataMatch();
//   }
// 
//   private void waitForDataGenerationTaskToComplete(Future<Long> dataGenerationTask)
//       throws Exception {
//     final Long dataGenerationDurationMinutes = dataGenerationTask.get(40, TimeUnit.MINUTES);
//     logger.info("Data generation took [{}] min", dataGenerationDurationMinutes);
//   }
// 
//   private Future<Long> startDataGeneration(int instanceCountToGenerate) {
//     final ExecutorService executor = Executors.newSingleThreadExecutor();
//     // when I start data generation and wait for it to finish
//     return executor.submit(
//         () -> {
//           // given I have data in the data
//           final OffsetDateTime beforeDataGeneration = OffsetDateTime.now();
//           final DataGenerationInformation dataGenerationInformation =
//               DataGenerationInformation.builder()
//                   .processInstanceCountToGenerate((long) instanceCountToGenerate)
//                   .decisionInstanceCountToGenerate((long) instanceCountToGenerate)
//                   .processDefinitionsAndNumberOfVersions(
//                       parseDefinitions(getDefaultDefinitionsOfClass(ProcessDataGenerator.class)))
//                   .decisionDefinitionsAndNumberOfVersions(
//                       parseDefinitions(getDefaultDefinitionsOfClass(DecisionDataGenerator.class)))
//                   .engineRestEndpoint(
//                       configurationService.getEngineRestApiEndpointOfCustomEngine("camunda-bpm"))
//                   .removeDeployments(false)
//                   .build();
//           final DataGenerationExecutor dataGenerationExecutor =
//               new DataGenerationExecutor(dataGenerationInformation);
//           dataGenerationExecutor.executeDataGeneration();
//           dataGenerationExecutor.awaitDataGenerationTermination();
//           return ChronoUnit.MINUTES.between(beforeDataGeneration, OffsetDateTime.now());
//         });
//   }
// }
