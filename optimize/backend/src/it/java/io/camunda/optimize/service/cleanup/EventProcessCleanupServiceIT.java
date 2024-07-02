/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.cleanup;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static java.util.stream.Collectors.groupingBy;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.fail;
//
// import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
// import io.camunda.optimize.dto.optimize.query.event.process.EventProcessInstanceDto;
// import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
// import io.camunda.optimize.service.util.IdGenerator;
// import io.camunda.optimize.service.util.configuration.cleanup.CleanupConfiguration;
// import io.camunda.optimize.service.util.configuration.cleanup.CleanupMode;
// import io.camunda.optimize.service.util.configuration.cleanup.ProcessCleanupConfiguration;
// import
// io.camunda.optimize.service.util.configuration.cleanup.ProcessDefinitionCleanupConfiguration;
// import java.time.OffsetDateTime;
// import java.util.List;
// import java.util.Map;
// import lombok.SneakyThrows;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class EventProcessCleanupServiceIT extends AbstractEventProcessIT {
//
//   @BeforeEach
//   public void enableCamundaCleanup() {
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getCleanupServiceConfiguration()
//         .getProcessDataCleanupConfiguration()
//         .setEnabled(true);
//   }
//
//   @Test
//   public void testCleanupModeAll() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(true);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final String instanceIdToGetCleanedUp =
//         ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     final String instanceIdToKeep = ingestStartAndEndEventWithSameTraceId(OffsetDateTime.now());
//     createAndPublishEventProcess();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final List<EventProcessInstanceDto> eventProcessInstances =
//         getEventProcessInstancesFromDatabase();
//     assertThat(eventProcessInstances)
//         .extracting(EventProcessInstanceDto::getProcessInstanceId)
//         .containsExactly(instanceIdToKeep);
//   }
//
//   @Test
//   public void testCleanupModeAll_specificKey() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(true);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final String instanceIdWithEndDateOlderThanDefaultTtl =
//         ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     final String instanceIdWithEndDateNewerThanDefaultTtl =
//         ingestStartAndEndEventWithSameTraceId(OffsetDateTime.now());
//     final String publishedProcessWithNoSpecialConfiguration = createAndPublishEventProcess();
//     final String publishedProcessWithSpecialConfiguration = createAndPublishEventProcess();
//     getProcessCleanupConfiguration()
//         .getProcessDefinitionSpecificConfiguration()
//         .put(
//             publishedProcessWithSpecialConfiguration,
//             ProcessDefinitionCleanupConfiguration.builder()
//                 .cleanupMode(CleanupMode.ALL)
//                 // higher ttl than default
//                 .ttl(getCleanupConfiguration().getTtl().plusYears(5L))
//                 .build());
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final Map<String, List<EventProcessInstanceDto>> eventProcessInstances =
//         getEventProcessInstancesFromDatabase().stream()
//             .collect(groupingBy(ProcessInstanceDto::getProcessDefinitionKey));
//     assertThat(eventProcessInstances.get(publishedProcessWithNoSpecialConfiguration))
//         .extracting(EventProcessInstanceDto::getProcessInstanceId)
//         .containsExactly(instanceIdWithEndDateNewerThanDefaultTtl);
//     assertThat(eventProcessInstances.get(publishedProcessWithSpecialConfiguration))
//         .extracting(EventProcessInstanceDto::getProcessInstanceId)
//         .containsExactlyInAnyOrder(
//             instanceIdWithEndDateNewerThanDefaultTtl, instanceIdWithEndDateOlderThanDefaultTtl);
//   }
//
//   @Test
//   public void testCleanupModeAll_disabled() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(false);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final String instanceIdToKeep =
//         ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     createAndPublishEventProcess();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final List<EventProcessInstanceDto> eventProcessInstances =
//         getEventProcessInstancesFromDatabase();
//     assertThat(eventProcessInstances)
//         .extracting(EventProcessInstanceDto::getProcessInstanceId)
//         .containsExactly(instanceIdToKeep);
//   }
//
//   @Test
//   public void testCleanupModelVariables() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(true);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     final String instanceIdWithCleanedVariables =
//         ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     final String instanceIdWithKeptVariables =
//         ingestStartAndEndEventWithSameTraceId(OffsetDateTime.now());
//     createAndPublishEventProcess();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final List<EventProcessInstanceDto> eventProcessInstances =
//         getEventProcessInstancesFromDatabase();
//     assertThat(eventProcessInstances)
//         .extracting(EventProcessInstanceDto::getProcessInstanceId)
//         .containsExactlyInAnyOrder(instanceIdWithKeptVariables, instanceIdWithCleanedVariables);
//     assertThat(eventProcessInstances)
//         .allSatisfy(
//             instance -> {
//               if (instanceIdWithKeptVariables.equals(instance.getProcessInstanceId())) {
//                 assertThat(instance.getVariables()).hasSize(1);
//               } else if (instanceIdWithCleanedVariables.equals(instance.getProcessInstanceId()))
// {
//                 assertThat(instance.getVariables()).isEmpty();
//               } else {
//                 fail("unexpected instance with id " + instance.getProcessInstanceId());
//               }
//             });
//   }
//
//   @Test
//   public void testCleanupModeVariables_specificKeyCleanupMode() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(true);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final String instanceIdToBeCleanup =
//         ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     final String instanceIdToNotCleanupAtAll =
//         ingestStartAndEndEventWithSameTraceId(OffsetDateTime.now());
//     final String publishedProcessWithNoSpecialConfiguration = createAndPublishEventProcess();
//     final String publishedProcessWithSpecialConfiguration = createAndPublishEventProcess();
//     getProcessCleanupConfiguration()
//         .getProcessDefinitionSpecificConfiguration()
//         .put(
//             publishedProcessWithSpecialConfiguration,
//             // variable mode for specific key
//             ProcessDefinitionCleanupConfiguration.builder()
//                 .cleanupMode(CleanupMode.VARIABLES)
//                 .build());
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final Map<String, List<EventProcessInstanceDto>> eventProcessInstances =
//         getEventProcessInstancesFromDatabase().stream()
//             .collect(groupingBy(ProcessInstanceDto::getProcessDefinitionKey));
//     assertThat(eventProcessInstances.get(publishedProcessWithNoSpecialConfiguration))
//         .extracting(ProcessInstanceDto::getProcessInstanceId)
//         .containsOnly(instanceIdToNotCleanupAtAll);
//     assertThat(eventProcessInstances.get(publishedProcessWithSpecialConfiguration))
//         .allSatisfy(
//             instance -> {
//               if (instanceIdToNotCleanupAtAll.equals(instance.getProcessInstanceId())) {
//                 assertThat(instance.getVariables()).hasSize(1);
//               } else if (instanceIdToBeCleanup.equals(instance.getProcessInstanceId())) {
//                 assertThat(instance.getVariables()).isEmpty();
//               } else {
//                 fail("unexpected instance with id " + instance.getProcessInstanceId());
//               }
//             });
//   }
//
//   @Test
//   public void testCleanupModeVariables_specificKeyTtl() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(true);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     final String instanceIdWithCleanedVariablesByDefaultConfig =
//         ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     final String instanceIdWithKeptVariables =
//         ingestStartAndEndEventWithSameTraceId(OffsetDateTime.now());
//     final String publishedProcessWithNoSpecialConfiguration = createAndPublishEventProcess();
//     final String publishedProcessWithSpecialConfiguration = createAndPublishEventProcess();
//     getProcessCleanupConfiguration()
//         .getProcessDefinitionSpecificConfiguration()
//         .put(
//             publishedProcessWithSpecialConfiguration,
//             ProcessDefinitionCleanupConfiguration.builder()
//                 .cleanupMode(CleanupMode.VARIABLES)
//                 // higher ttl than default
//                 .ttl(getCleanupConfiguration().getTtl().plusYears(5L))
//                 .build());
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final Map<String, List<EventProcessInstanceDto>> eventProcessInstances =
//         getEventProcessInstancesFromDatabase().stream()
//             .collect(groupingBy(ProcessInstanceDto::getProcessDefinitionKey));
//     assertThat(eventProcessInstances.get(publishedProcessWithNoSpecialConfiguration))
//         .allSatisfy(
//             instance -> {
//               if (instanceIdWithKeptVariables.equals(instance.getProcessInstanceId())) {
//                 assertThat(instance.getVariables()).hasSize(1);
//               } else if (instanceIdWithCleanedVariablesByDefaultConfig.equals(
//                   instance.getProcessInstanceId())) {
//                 assertThat(instance.getVariables()).isEmpty();
//               } else {
//                 fail("unexpected instance with id " + instance.getProcessInstanceId());
//               }
//             });
//     assertThat(eventProcessInstances.get(publishedProcessWithSpecialConfiguration))
//         .extracting(EventProcessInstanceDto::getVariables)
//         .allSatisfy(variables -> assertThat(variables).hasSize(1));
//   }
//
//   @Test
//   @SneakyThrows
//   public void testCleanupModelVariables_disabled() {
//     // given
//     getProcessCleanupConfiguration().setEnabled(false);
//     getProcessCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     ingestStartAndEndEventWithSameTraceId(getEndTimeLessThanGlobalTtl());
//     createAndPublishEventProcess();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     final List<EventProcessInstanceDto> eventProcessInstances =
//         getEventProcessInstancesFromDatabase();
//     assertThat(eventProcessInstances).extracting(ProcessInstanceDto::getVariables).hasSize(1);
//   }
//
//   private String createAndPublishEventProcess() {
//     final String eventProcessMappingId =
//         createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);
//     publishEventProcess(eventProcessMappingId);
//     return eventProcessMappingId;
//   }
//
//   private String ingestStartAndEndEventWithSameTraceId(
//       final OffsetDateTime endTimeLessThanGlobalTtl) {
//     final String traceId = IdGenerator.getNextId();
//     ingestTestEvent(STARTED_EVENT, endTimeLessThanGlobalTtl, traceId);
//     ingestTestEvent(FINISHED_EVENT, endTimeLessThanGlobalTtl, traceId);
//     return traceId;
//   }
//
//   protected OffsetDateTime getEndTimeLessThanGlobalTtl() {
//     return OffsetDateTime.now().minus(getCleanupConfiguration().getTtl()).minusSeconds(1);
//   }
//
//   private CleanupConfiguration getCleanupConfiguration() {
//     return embeddedOptimizeExtension.getConfigurationService().getCleanupServiceConfiguration();
//   }
//
//   protected ProcessCleanupConfiguration getProcessCleanupConfiguration() {
//     return getCleanupConfiguration().getProcessDataCleanupConfiguration();
//   }
// }
