/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.importing.user_task;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_INDEX_PREFIX;
// import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
// import static io.camunda.optimize.util.BpmnModels.USER_TASK_1;
// import static io.camunda.optimize.util.BpmnModels.USER_TASK_2;
// import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
// import static jakarta.ws.rs.HttpMethod.POST;
// import static java.util.stream.Collectors.toList;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockserver.model.HttpRequest.request;
// import static org.mockserver.model.StringBody.subString;
//
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
// import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
// import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.importing.ImportMediator;
// import io.camunda.optimize.service.importing.engine.EngineImportScheduler;
// import
// io.camunda.optimize.service.importing.engine.mediator.CompletedActivityInstanceEngineImportMediator;
// import
// io.camunda.optimize.service.importing.engine.mediator.CompletedProcessInstanceEngineImportMediator;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.util.BpmnModels;
// import java.io.IOException;
// import java.time.OffsetDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Locale;
// import java.util.Optional;
// import java.util.UUID;
// import java.util.concurrent.TimeUnit;
// import java.util.stream.Collectors;
// import lombok.SneakyThrows;
// import org.assertj.core.groups.Tuple;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
// import org.mockserver.integration.ClientAndServer;
// import org.mockserver.matchers.Times;
// import org.mockserver.model.HttpError;
// import org.mockserver.model.HttpRequest;
//
// @Tag(OPENSEARCH_PASSING)
// public class UserTaskImportIT extends AbstractUserTaskImportIT {
//
//   @Test
//   public void completedUserTasksAreImported() {
//     // given
//     deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks()).hasSize(2);
//               assertThat(
//                       processInstanceDto.getUserTasks().stream()
//                           .map(FlowNodeInstanceDto::getFlowNodeId)
//                           .collect(toList()))
//                   .containsExactlyInAnyOrder(USER_TASK_1, USER_TASK_2);
//               processInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       simpleUserTaskInstanceDto -> {
//
// assertThat(simpleUserTaskInstanceDto.getUserTaskInstanceId()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getFlowNodeId()).isNotNull();
//
// assertThat(simpleUserTaskInstanceDto.getFlowNodeInstanceId()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getStartDate()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getEndDate()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getDueDate()).isNull();
//                         assertThat(simpleUserTaskInstanceDto.getDeleteReason()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getTotalDurationInMs()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getIdleDurationInMs()).isNotNull();
//                         assertThat(simpleUserTaskInstanceDto.getWorkDurationInMs()).isNotNull();
//
// assertThat(simpleUserTaskInstanceDto.getAssigneeOperations()).isNotNull();
//                       });
//             });
//   }
//
//   @Test
//   public void runningUserTaskIsImported() {
//     // given (two user tasks, one is started)
//     deployAndStartTwoUserTasksProcess();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks())
//                   .singleElement()
//                   .satisfies(
//                       userTask -> {
//                         assertThat(userTask.getFlowNodeId()).isEqualTo(USER_TASK_1);
//                         assertThat(userTask.getUserTaskInstanceId()).isNotNull();
//                         assertThat(userTask.getFlowNodeId()).isNotNull();
//                         assertThat(userTask.getFlowNodeInstanceId()).isNotNull();
//                         assertThat(userTask.getStartDate()).isNotNull();
//                         assertThat(userTask.getEndDate()).isNull();
//                         assertThat(userTask.getTotalDurationInMs()).isNull();
//                       });
//             });
//   }
//
//   @Test
//   public void canceledUserTaskIsImported() {
//     // given
//     final ProcessInstanceEngineDto processInstance = deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.cancelActivityInstance(processInstance.getId(), USER_TASK_1);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto ->
//                 assertThat(processInstanceDto.getUserTasks())
//                     .singleElement()
//                     .satisfies(
//                         userTask -> {
//                           assertThat(userTask.getFlowNodeId()).isEqualTo(USER_TASK_1);
//                           assertThat(userTask.getUserTaskInstanceId()).isNotNull();
//                           assertThat(userTask.getFlowNodeInstanceId()).isNotNull();
//                           assertThat(userTask.getStartDate()).isNotNull();
//                           assertThat(userTask.getEndDate()).isNotNull();
//                           assertThat(userTask.getCanceled()).isTrue();
//                         }));
//   }
//
//   @Test
//   public void canceledUserTaskDoesNotAffectOtherInstance() {
//     // given
//     final ProcessInstanceEngineDto firstInstance = deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.cancelActivityInstance(firstInstance.getId(), USER_TASK_1);
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.startProcessInstance(firstInstance.getDefinitionId());
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> allProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(allProcessInstances)
//         .hasSize(2)
//         .filteredOn(
//             savedInstance -> savedInstance.getProcessInstanceId().equals(firstInstance.getId()))
//         .singleElement()
//         .satisfies(
//             instance ->
//                 assertThat(instance.getUserTasks())
//                     .singleElement()
//                     .satisfies(
//                         task -> {
//                           assertThat(task.getFlowNodeId()).isEqualTo(USER_TASK_1);
//                           assertThat(task.getCanceled()).isTrue();
//                         }));
//     assertThat(allProcessInstances)
//         .filteredOn(
//             savedInstance -> savedInstance.getProcessInstanceId().equals(secondInstance.getId()))
//         .singleElement()
//         .satisfies(
//             instance ->
//                 assertThat(instance.getUserTasks())
//                     .singleElement()
//                     .satisfies(
//                         task -> {
//                           assertThat(task.getFlowNodeId()).isEqualTo(USER_TASK_1);
//                           assertThat(task.getCanceled()).isFalse();
//                         }));
//   }
//
//   @Test
//   public void canceledAndCompletedUserTasksAreImported() {
//     // given
//     final ProcessInstanceEngineDto processInstance = deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.cancelActivityInstance(processInstance.getId(), USER_TASK_2);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto ->
//                 assertThat(processInstanceDto.getUserTasks())
//                     .hasSize(2)
//                     .allSatisfy(
//                         userTask -> {
//                           assertThat(userTask.getUserTaskInstanceId()).isNotNull();
//                           assertThat(userTask.getFlowNodeId()).isNotNull();
//                           assertThat(userTask.getStartDate()).isNotNull();
//                           assertThat(userTask.getEndDate()).isNotNull();
//                         })
//                     .extracting(
//                         FlowNodeInstanceDto::getFlowNodeId, FlowNodeInstanceDto::getCanceled)
//                     .containsExactlyInAnyOrder(
//                         Tuple.tuple(USER_TASK_1, false), Tuple.tuple(USER_TASK_2, true)));
//   }
//
//   @Test
//   public void runningAndCompletedUserTasksAreImported() {
//     // given
//     deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks())
//                   .hasSize(2)
//                   .extracting(FlowNodeInstanceDto::getFlowNodeId)
//                   .containsExactlyInAnyOrder(USER_TASK_1, USER_TASK_2);
//               processInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       userTask -> {
//                         if (USER_TASK_1.equals(userTask.getFlowNodeId())) {
//                           assertThat(userTask.getEndDate()).isNotNull();
//                         } else {
//                           assertThat(userTask.getEndDate()).isNull();
//                         }
//                       });
//             });
//   }
//
//   @Test
//   public void runningAndCompletedUserTasksAreImported_despiteEsUpdateFailures() {
//     // given
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances()).isEmpty();
//
//     // given ES update request fails
//     deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     final ClientAndServer dbMockServer = useAndGetDbMockServer();
//     final HttpRequest userTaskImportMatcher =
//         request()
//             .withPath("/_bulk")
//             .withMethod(POST)
//             .withBody(
//                 subString(
//                     "\"_index\":\""
//                         + embeddedOptimizeExtension
//                             .getOptimizeDatabaseClient()
//                             .getIndexNameService()
//                             .getIndexPrefix()
//                         + "-"
//                         + PROCESS_INSTANCE_INDEX_PREFIX));
//     dbMockServer
//         .when(userTaskImportMatcher, Times.once())
//         .error(HttpError.error().withDropConnection(true));
//
//     // when
//     importAllEngineEntitiesFromLastIndex();
//
//     // then expected user tasks are stored on next successful update
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks())
//                   .hasSize(2)
//                   .extracting(FlowNodeInstanceDto::getFlowNodeId)
//                   .containsExactlyInAnyOrder(USER_TASK_1, USER_TASK_2);
//               processInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       userTask -> {
//                         if (USER_TASK_1.equals(userTask.getFlowNodeId())) {
//                           assertThat(userTask.getEndDate()).isNotNull();
//                         } else {
//                           assertThat(userTask.getEndDate()).isNull();
//                         }
//                       });
//             });
//     dbMockServer.verify(userTaskImportMatcher);
//   }
//
//   @Test
//   public void onlyUserTasksRelatedToProcessInstancesAreImported() throws IOException {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     final UUID independentUserTaskId = engineIntegrationExtension.createIndependentUserTask();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             processInstanceDto ->
//                 assertThat(processInstanceDto.getUserTasks())
//                     .singleElement()
//                     .satisfies(
//                         userTask -> {
//                           assertThat(userTask.getFlowNodeId()).isEqualTo(USER_TASK_1);
//                           assertThat(userTask.getFlowNodeInstanceId())
//                               .isNotEqualTo(independentUserTaskId.toString());
//                         }));
//   }
//
//   @Test
//   public void importFinishesIfIndependentRunningUserTasksExist() throws IOException {
//     // given
//     engineIntegrationExtension.createIndependentUserTask();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances()).isEmpty();
//   }
//
//   @Test
//   public void noSideEffectsByOtherProcessInstanceUserTasks() {
//     // given
//     final ProcessInstanceEngineDto processInstanceDto1 = deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     // only first task finished
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .hasSize(2)
//         .allSatisfy(
//             persistedProcessInstanceDto -> {
//               if (persistedProcessInstanceDto
//                   .getProcessInstanceId()
//                   .equals(processInstanceDto1.getId())) {
//                 assertThat(persistedProcessInstanceDto.getUserTasks()).hasSize(2);
//                 assertThat(
//                         persistedProcessInstanceDto.getUserTasks().stream()
//                             .map(FlowNodeInstanceDto::getFlowNodeId)
//                             .collect(toList()))
//                     .containsExactlyInAnyOrder(USER_TASK_1, USER_TASK_2);
//               } else {
//                 assertThat(persistedProcessInstanceDto.getUserTasks())
//                     .singleElement()
//                     .satisfies(
//                         userTask -> assertThat(userTask.getFlowNodeId()).isEqualTo(USER_TASK_1));
//               }
//             });
//   }
//
//   @Test
//   public void importFinishesIfIndependentCompletesUserTasksWithOperationsExist()
//       throws IOException {
//     // given
//     engineIntegrationExtension.createIndependentUserTask();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances()).isEmpty();
//   }
//
//   @Test
//   public void idleTimeMetricIsCalculatedOnClaimOperationImport() {
//     // given
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     final long idleDuration = 500;
//     changeUserTaskIdleDuration(processInstanceDto, idleDuration);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             persistedProcessInstanceDto -> {
//               persistedProcessInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       userTask ->
//                           assertThat(userTask.getIdleDurationInMs()).isEqualTo(idleDuration));
//             });
//   }
//
//   @Test
//   public void workAndIdleTimeCalculationsForMultipleClaimOperations() {
//     // given a userTask that is claimed and unclaimed multiple times before completion
//     final OffsetDateTime now = dateFreezer().freezeDateAndReturn();
//     final String firstClaimUser = "firstClaimUser";
//     final String secondClaimUser = "secondClaimUser";
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//
//     // timeline:
//     // start userTask now --+5secs--> claim --+4secs--> unclaim --+3secs--> claim --+2secs-->
//     // finish.
//     changeUserTaskStartTime(processInstanceDto, now);
//     importAllEngineEntitiesFromScratch();
//
//     // claim usertask after 5s
//     engineIntegrationExtension.claimAllRunningUserTasksWithAssignee(
//         firstClaimUser, processInstanceDto.getId());
//     engineIntegrationExtension.claimAllRunningUserTasksWithAssignee(
//         firstClaimUser, processInstanceDto.getId());
//     final long firstIdleDuration = 500;
//     final OffsetDateTime timeOfFirstClaim = now.plus(firstIdleDuration, ChronoUnit.MILLIS);
//     changeClaimTimestampForAssigneeId(processInstanceDto, timeOfFirstClaim, firstClaimUser);
//     importAllEngineEntitiesFromScratch();
//
//     // unclaim 4s later
//     engineIntegrationExtension.unclaimAllRunningUserTasks();
//     final long firstWorkDuration = 400;
//     final OffsetDateTime timeOfFirstUnclaim =
//         timeOfFirstClaim.plus(firstWorkDuration, ChronoUnit.MILLIS);
//     changeUnclaimTimestampForAssigneeId(processInstanceDto, timeOfFirstUnclaim, firstClaimUser);
//     importAllEngineEntitiesFromScratch();
//
//     // claim 3s later
//     engineIntegrationExtension.claimAllRunningUserTasksWithAssignee(
//         secondClaimUser, processInstanceDto.getId());
//     final long secondIdleDuration = 300;
//     final OffsetDateTime timeOfSecondClaim =
//         timeOfFirstUnclaim.plus(secondIdleDuration, ChronoUnit.MILLIS);
//     changeClaimTimestampForAssigneeId(processInstanceDto, timeOfSecondClaim, secondClaimUser);
//     importAllEngineEntitiesFromScratch();
//
//     // finish 2s after last claim
//     engineIntegrationExtension.completeUserTaskWithoutClaim(processInstanceDto.getId());
//     final long secondWorkDuration = 200;
//     final OffsetDateTime timeOfFinish =
//         timeOfSecondClaim.plus(secondWorkDuration, ChronoUnit.MILLIS);
//     changeUserTaskEndTime(processInstanceDto, timeOfFinish);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     // workDuration is the sum of all durations during which the usertask was assigned and
//     // idleDuration is the sum of all durations during which the userTask was unassigned
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             persistedProcessInstanceDto -> {
//               persistedProcessInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       userTask -> {
//                         assertThat(userTask.getWorkDurationInMs())
//                             .isEqualTo(firstWorkDuration + secondWorkDuration);
//                         assertThat(userTask.getIdleDurationInMs())
//                             .isEqualTo(firstIdleDuration + secondIdleDuration);
//                       });
//             });
//   }
//
//   @Test
//   public void defaultTimesOnCompletionWithNoClaimOperation() {
//     // given a usertask that has been started and then completed with no claim/unclaim operations
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.completeUserTaskWithoutClaim(processInstanceDto.getId());
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then the usertask is assumed to be worked on programmatically only via API,
//     // meaning totalDuration == workDuration and idleDuration == null
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .flatExtracting(ProcessInstanceDto::getUserTasks)
//         .allSatisfy(
//             userTask -> {
//               assertThat(userTask.getWorkDurationInMs()).isNotNull();
//               assertThat(userTask.getIdleDurationInMs()).isZero();
//
// assertThat(userTask.getWorkDurationInMs()).isEqualTo(userTask.getTotalDurationInMs());
//             });
//   }
//
//   @Test
//   public void defaultTimesOnCancellationWithNoClaimOperation() {
//     // given a userTask that has been started and then cancelled with no claim/unclaim
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.cancelActivityInstance(processInstanceDto.getId(), USER_TASK_1);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then the usertask is assumed to have been idle the entire time,
//     // meaning idle == total and work == 0
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .hasSize(1)
//         .flatExtracting(ProcessInstanceDto::getUserTasks)
//         .singleElement()
//         .satisfies(
//             userTask -> {
//               assertThat(userTask.getTotalDurationInMs()).isNotNull();
//               assertThat(userTask.getWorkDurationInMs()).isNotNull().isZero();
//
// assertThat(userTask.getIdleDurationInMs()).isEqualTo(userTask.getTotalDurationInMs());
//             });
//   }
//
//   @Test
//   public void metricsScriptOnlyAppliedOnUpdatedUserTasks() {
//     // given
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // with first import round, first userTask is updated
//     importAllEngineEntitiesFromScratch();
//     // change first userTask durations to be able to assert no update happens in second import
// round
//     databaseIntegrationTestExtension.updateUserTaskDurations(
//         processInstanceDto.getId(), processInstanceDto.getProcessDefinitionKey(), 99999L);
//
//     // when completing userTask 2 as well so the next import round has updates for userTask2 but
// not
//     // for userTask1
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     importAllEngineEntitiesFromLastIndex();
//
//     // then the durations for userTask1 are not recalculated because it was not updated in the
//     // second round
//     final List<ProcessInstanceDto> instances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .hasSize(1)
//         .flatExtracting(ProcessInstanceDto::getUserTasks)
//         .hasSize(2);
//     final Optional<FlowNodeInstanceDto> task1 =
//         instances.get(0).getUserTasks().stream()
//             .filter(task -> USER_TASK_1.equals(task.getFlowNodeId()))
//             .findFirst();
//     final Optional<FlowNodeInstanceDto> task2 =
//         instances.get(0).getUserTasks().stream()
//             .filter(task -> USER_TASK_2.equals(task.getFlowNodeId()))
//             .findFirst();
//
//     assertThat(task1)
//         .isPresent()
//         .get()
//         .satisfies(
//             userTask -> {
//               assertThat(userTask.getTotalDurationInMs()).isEqualTo(99999L);
//               assertThat(userTask.getWorkDurationInMs()).isEqualTo(99999L);
//               assertThat(userTask.getIdleDurationInMs()).isEqualTo(99999L);
//             });
//     assertThat(task2)
//         .isPresent()
//         .get()
//         .satisfies(
//             userTask -> {
//               assertThat(userTask.getTotalDurationInMs()).isNotEqualTo(99999L);
//               assertThat(userTask.getWorkDurationInMs()).isNotEqualTo(99999L);
//               assertThat(userTask.getIdleDurationInMs()).isNotEqualTo(99999L);
//             });
//   }
//
//   @ParameterizedTest
//   @MethodSource("allUserTaskReports")
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void userTaskFrequencyReportsCanBeEvaluatedWithOnlyCancellationUserTaskDataImported(
//       ProcessReportDataType reportDataType) {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     final ProcessInstanceEngineDto processInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.cancelActivityInstance(processInstance.getId(), USER_TASK_1);
//
//     importCompletedActivityAndProcessInstances();
//
//     // when
//     ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setProcessDefinitionKey(processDefinition.getKey())
//             .setProcessDefinitionVersion(processDefinition.getVersionAsString())
//             .setReportDataType(reportDataType)
//             .setDistributeByDateInterval(AggregateByDateUnit.AUTOMATIC)
//             .setGroupByDateInterval(AggregateByDateUnit.AUTOMATIC)
//             .build();
//     final ReportResultResponseDto<Object> result =
//         reportClient.evaluateReport(reportData).getResult();
//
//     // then the report can be evaluated even though user task only contains cancellation data
//     assertThat(result.getInstanceCount()).isEqualTo(1);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
//   }
//
//   @Test
//   public void userTasksWithoutProcessDefinitionKeyCanBeImported() {
//     // given a completed and a running userTask without definitionKey
//     deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineDatabaseExtension.removeProcessDefinitionKeyFromAllHistoricFlowNodes();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final List<ProcessInstanceDto> storedProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(storedProcessInstances)
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks())
//                   .hasSize(2)
//                   .extracting(FlowNodeInstanceDto::getFlowNodeId)
//                   .containsExactlyInAnyOrder(USER_TASK_1, USER_TASK_2);
//             });
//   }
//
//   private static List<ProcessReportDataType> allUserTaskReports() {
//     return Arrays.stream(ProcessReportDataType.values())
//         .filter(type -> type.name().toLowerCase(Locale.ENGLISH).startsWith("user_task_"))
//         .collect(Collectors.toList());
//   }
//
//   @SneakyThrows
//   private void importCompletedActivityAndProcessInstances() {
//     for (EngineImportScheduler scheduler :
//         embeddedOptimizeExtension.getImportSchedulerManager().getEngineImportSchedulers()) {
//       final List<ImportMediator> mediators =
//           scheduler.getImportMediators().stream()
//               .filter(
//                   mediator ->
//                       CompletedActivityInstanceEngineImportMediator.class.equals(
//                               mediator.getClass())
//                           || CompletedProcessInstanceEngineImportMediator.class.equals(
//                               mediator.getClass()))
//               .collect(Collectors.toList());
//       for (ImportMediator mediator : mediators) {
//         mediator.runImport().get(10, TimeUnit.SECONDS);
//       }
//     }
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//   }
// }
