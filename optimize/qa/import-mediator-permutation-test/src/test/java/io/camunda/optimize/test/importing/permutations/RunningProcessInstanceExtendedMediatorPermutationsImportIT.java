/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.test.importing.permutations;
//
// import static io.camunda.optimize.dto.optimize.ProcessInstanceConstants.ACTIVE_STATE;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static io.camunda.optimize.util.BpmnModels.USER_TASK_1;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.tuple;
//
// import com.google.common.collect.ImmutableList;
// import com.google.common.collect.Lists;
// import io.camunda.optimize.dto.optimize.query.event.process.CamundaActivityEventDto;
// import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.importing.ImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.CompletedActivityInstanceEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.CompletedProcessInstanceEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.CompletedUserTaskEngineImportMediator;
// import io.camunda.optimize.service.importing.engine.mediator.IdentityLinkLogEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.RunningActivityInstanceEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.RunningProcessInstanceEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.RunningUserTaskInstanceEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.UserOperationLogEngineImportMediator;
// import io.camunda.optimize.service.importing.engine.mediator.VariableUpdateEngineImportMediator;
// import java.util.List;
// import java.util.stream.Stream;
// import lombok.extern.slf4j.Slf4j;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
//
// @Slf4j
// public class RunningProcessInstanceExtendedMediatorPermutationsImportIT
//     extends AbstractExtendedImportMediatorPermutationsIT {
//
//   @BeforeAll
//   public static void given() {
//     // given
//     final ProcessInstanceEngineDto processInstanceDto = deployAndStartUserTaskProcess();
//     engineIntegrationExtension.claimAllRunningUserTasks(processInstanceDto.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(
//         processInstanceDto.getId(), CANDIDATE_GROUP);
//   }
//
//   @Test
//   public void runningInstanceIsFullyImported() {
//     runningActivityRelatedMediators()
//         .forEach(
//             mediatorOrder -> {
//               logMediatorOrder(log, mediatorOrder);
//
//               // includes completed events in case CompletedActivityInstanceEngineImportMediator
// is
//               // present
//               int numberOfEventsToImport =
//                   mediatorOrder.contains(CompletedActivityInstanceEngineImportMediator.class)
//                       ? 2
//                       : 1;
//
//               // when
//               performOrderedImport(mediatorOrder);
//               databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//               // then
//               assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//                   .hasSize(1)
//                   .singleElement()
//                   .satisfies(
//                       persistedProcessInstanceDto -> {
//                         // general instance sanity check
//                         assertThat(persistedProcessInstanceDto.getStartDate()).isNotNull();
//                         assertThat(persistedProcessInstanceDto.getEndDate()).isNull();
//
// assertThat(persistedProcessInstanceDto.getState()).isEqualTo(ACTIVE_STATE);
//
//                         assertThat(persistedProcessInstanceDto.getFlowNodeInstances())
//                             .hasSize(numberOfEventsToImport)
//                             .allSatisfy(activity ->
// assertThat(activity.getStartDate()).isNotNull())
//                             .extracting(
//                                 FlowNodeInstanceDto::getFlowNodeId,
//                                 FlowNodeInstanceDto::getEndDate,
//                                 FlowNodeInstanceDto::getTotalDurationInMs)
//                             .contains(tuple(USER_TASK_1, null, null));
//                         assertThat(persistedProcessInstanceDto.getUserTasks())
//                             .hasSize(1)
//                             .singleElement()
//                             .satisfies(
//                                 userTask -> {
//                                   assertThat(userTask.getStartDate()).isNotNull();
//                                   assertThat(userTask.getEndDate()).isNull();
//                                   assertThat(userTask.getAssignee()).isEqualTo(DEFAULT_USERNAME);
//                                   assertThat(userTask.getCandidateGroups())
//                                       .containsOnly(CANDIDATE_GROUP);
//                                   assertThat(userTask.getIdleDurationInMs()).isGreaterThan(0L);
//                                   assertThat(userTask.getWorkDurationInMs()).isNull();
//                                   assertThat(userTask.getAssigneeOperations()).hasSize(1);
//                                 });
//                       });
//
//               final List<CamundaActivityEventDto> allStoredCamundaActivityEventsForDefinition =
//
// databaseIntegrationTestExtension.getAllStoredCamundaActivityEventsForDefinition(
//                       TEST_PROCESS);
//               // the process instance start and the single running user task event
//               // + startEvent in case CompletedActivityInstanceEngineImportMediator is present
//               assertThat(allStoredCamundaActivityEventsForDefinition)
//                   .hasSize(1 + numberOfEventsToImport);
//             });
//   }
//
//   private static Stream<List<Class<? extends ImportMediator>>> runningActivityRelatedMediators()
// {
//     return getMediatorPermutationsStream(
//         ImmutableList.of(
//             RunningActivityInstanceEngineImportMediator.class,
//             RunningUserTaskInstanceEngineImportMediator.class,
//             RunningProcessInstanceEngineImportMediator.class,
//             VariableUpdateEngineImportMediator.class,
//             IdentityLinkLogEngineImportMediator.class),
//         Lists.newArrayList(
//             CompletedActivityInstanceEngineImportMediator.class,
//             CompletedUserTaskEngineImportMediator.class,
//             CompletedProcessInstanceEngineImportMediator.class,
//             UserOperationLogEngineImportMediator.class));
//   }
// }
