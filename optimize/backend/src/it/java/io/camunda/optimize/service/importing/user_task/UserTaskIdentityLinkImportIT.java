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
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static
// io.camunda.optimize.service.util.InstanceIndexUtil.getProcessInstanceIndexAliasName;
// import static
// io.camunda.optimize.service.util.importing.EngineConstants.IDENTITY_LINK_OPERATION_ADD;
// import static
// io.camunda.optimize.service.util.importing.EngineConstants.IDENTITY_LINK_OPERATION_DELETE;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.google.common.collect.ImmutableSet;
// import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
// import io.camunda.optimize.dto.optimize.persistence.AssigneeOperationDto;
// import io.camunda.optimize.dto.optimize.persistence.CandidateGroupOperationDto;
// import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.util.BpmnModels;
// import java.io.IOException;
// import java.time.OffsetDateTime;
// import java.util.Arrays;
// import java.util.List;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class UserTaskIdentityLinkImportIT extends AbstractUserTaskImportIT {
//
//   @Test
//   public void importOfUserTaskWorkerDataCanBeDisabled() throws IOException {
//     // given
//
// embeddedOptimizeExtension.getConfigurationService().setImportUserTaskWorkerDataEnabled(false);
//     embeddedOptimizeExtension.reloadConfiguration();
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     String defaultCandidateGroup = "defaultCandidateGroupId";
//     engineIntegrationExtension.createGroup(defaultCandidateGroup);
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(defaultCandidateGroup);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               processInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       task -> {
//                         assertThat(task.getAssignee()).isNull();
//                         assertThat(task.getCandidateGroups()).isEmpty();
//                         assertThat(task.getCandidateGroupOperations()).isEmpty();
//                         assertThat(task.getAssigneeOperations()).isEmpty();
//                       });
//             });
//   }
//
//   @Test
//   public void identityLinksLogsAreImported() throws Exception {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     String defaultCandidateGroup = "defaultCandidateGroupId";
//     engineIntegrationExtension.createGroup(defaultCandidateGroup);
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(defaultCandidateGroup);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               processInstanceDto
//                   .getUserTasks()
//                   .forEach(
//                       userTask -> {
//                         assertThat(userTask.getAssignee()).isEqualTo(DEFAULT_USERNAME);
//                         assertThat(userTask.getCandidateGroups())
//                             .containsExactly(defaultCandidateGroup);
//                         assertThat(userTask.getAssigneeOperations()).hasSize(1);
//                         userTask
//                             .getAssigneeOperations()
//                             .forEach(
//                                 assigneeOperationDto -> {
//                                   assertThat(assigneeOperationDto.getId()).isNotNull();
//                                   assertThat(assigneeOperationDto.getUserId())
//                                       .isEqualTo(DEFAULT_USERNAME);
//                                   assertThat(assigneeOperationDto.getTimestamp()).isNotNull();
//                                   assertThat(assigneeOperationDto.getOperationType())
//                                       .isEqualTo(IDENTITY_LINK_OPERATION_ADD);
//                                 });
//                         assertThat(userTask.getCandidateGroupOperations()).hasSize(1);
//                         userTask
//                             .getCandidateGroupOperations()
//                             .forEach(
//                                 candidateGroupOperationDto -> {
//                                   assertThat(candidateGroupOperationDto.getId()).isNotNull();
//                                   assertThat(candidateGroupOperationDto.getGroupId())
//                                       .isEqualTo(defaultCandidateGroup);
//
// assertThat(candidateGroupOperationDto.getTimestamp()).isNotNull();
//                                   assertThat(candidateGroupOperationDto.getOperationType())
//                                       .isEqualTo(IDENTITY_LINK_OPERATION_ADD);
//                                 });
//                       });
//             });
//   }
//
//   @Test
//   public void changingAssigneeWithIdenticalLinkLogTimestampsResolvesCorrectAssignee()
//       throws Exception {
//     // background: changing the assignee in tasklist results in those two assignee operations,
// which
//     // will have the
//     // exact same timestamp. We need to make sure that the add operation always wins over the
// delete
//     // if the timestamp
//     // is identical.
//
//     // given
//     engineIntegrationExtension.addUser("kermit", "foo");
//     engineIntegrationExtension.grantAllAuthorizations("kermit");
//     ProcessInstanceEngineDto instanceDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.claimAllRunningUserTasks();
//     engineIntegrationExtension.unclaimAllRunningUserTasks();
//     engineIntegrationExtension.claimAllRunningUserTasks("kermit", "foo", instanceDto.getId());
//     // we need to make sure that the new timestamp is after the first claim, since
//     // otherwise the ordering of the assignee operations won't be correct.
//     OffsetDateTime timestamp = OffsetDateTime.now().plusHours(1);
//     engineDatabaseExtension.changeLinkLogTimestampForLastTwoAssigneeOperations(timestamp);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto ->
//                 processInstanceDto
//                     .getUserTasks()
//                     .forEach(userTask ->
// assertThat(userTask.getAssignee()).isEqualTo("kermit")));
//   }
//
//   @Test
//   public void assigneeIsCorrectlyDeterminedForMultipleUserTasks() throws Exception {
//     // given
//     deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.addUser("secondUser", "fooPassword");
//     engineIntegrationExtension.grantAllAuthorizations("secondUser");
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks("secondUser", "fooPassword");
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks())
//                   .hasSize(2)
//                   .extracting(FlowNodeInstanceDto::getAssignee)
//                   .containsOnlyOnceElementsOf(ImmutableSet.of(DEFAULT_USERNAME, "secondUser"));
//             });
//   }
//
//   @Test
//   public void candidateGroupIsCorrectlyDeterminedForMultipleUserTasks() throws Exception {
//     // given
//     deployAndStartTwoUserTasksProcess();
//     engineIntegrationExtension.createGroup("firstGroup");
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("firstGroup");
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.createGroup("secondGroup");
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("secondGroup");
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks())
//                   .hasSize(2)
//                   .flatExtracting(FlowNodeInstanceDto::getCandidateGroups)
//                   .containsOnlyOnceElementsOf(ImmutableSet.of("firstGroup", "secondGroup"));
//             });
//   }
//
//   @Test
//   public void severalAssigneeOperationsLeadToCorrectResult() throws Exception {
//     // given
//     final ProcessInstanceEngineDto instanceDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.claimAllRunningUserTasks();
//     engineIntegrationExtension.unclaimAllRunningUserTasks();
//     engineIntegrationExtension.addUser("secondUser", "secondPassword");
//     engineIntegrationExtension.grantAllAuthorizations("secondUser");
//     engineIntegrationExtension.finishAllRunningUserTasks("secondUser", "secondPassword");
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               List<FlowNodeInstanceDto> userTasks = processInstanceDto.getUserTasks();
//               assertThat(userTasks).hasSize(1);
//               List<AssigneeOperationDto> assigneeOperations =
//                   userTasks.get(0).getAssigneeOperations();
//               assertThat(assigneeOperations.get(0).getOperationType())
//                   .isEqualTo(IDENTITY_LINK_OPERATION_ADD);
//               assertThat(assigneeOperations.get(1).getOperationType())
//                   .isEqualTo(IDENTITY_LINK_OPERATION_DELETE);
//               assertThat(assigneeOperations.get(2).getOperationType())
//                   .isEqualTo(IDENTITY_LINK_OPERATION_ADD);
//               assertThat(userTasks.get(0).getAssignee()).isEqualTo("secondUser");
//             });
//   }
//
//   @Test
//   public void assigneeWithoutClaimIsNull() throws Exception {
//     // given
//     ProcessInstanceEngineDto engineDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.completeUserTaskWithoutClaim(engineDto.getId());
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               List<FlowNodeInstanceDto> userTasks = processInstanceDto.getUserTasks();
//               assertThat(userTasks).hasSize(1);
//               assertThat(userTasks.get(0).getAssignee()).isNull();
//             });
//   }
//
//   @Test
//   public void assigneeCanBeDeterminedForStillRunningUserTasks() throws Exception {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.claimAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               List<FlowNodeInstanceDto> userTasks = processInstanceDto.getUserTasks();
//               assertThat(userTasks).hasSize(1);
//               assertThat(userTasks.get(0).getAssignee()).isEqualTo(DEFAULT_USERNAME);
//             });
//   }
//
//   @Test
//   public void severalCandidateGroupOperationsLeadToCorrectResult() throws Exception {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.createGroup("firstGroup");
//     engineIntegrationExtension.createGroup("secondGroup");
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("firstGroup");
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("secondGroup");
//     engineIntegrationExtension.deleteCandidateGroupForAllRunningUserTasks("firstGroup");
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               List<FlowNodeInstanceDto> userTasks = processInstanceDto.getUserTasks();
//               assertThat(userTasks).hasSize(1);
//               List<CandidateGroupOperationDto> candidateGroupOperations =
//                   userTasks.get(0).getCandidateGroupOperations();
//               assertThat(candidateGroupOperations.get(0).getOperationType())
//                   .isEqualTo(IDENTITY_LINK_OPERATION_ADD);
//               assertThat(candidateGroupOperations.get(1).getOperationType())
//                   .isEqualTo(IDENTITY_LINK_OPERATION_ADD);
//               assertThat(candidateGroupOperations.get(2).getOperationType())
//                   .isEqualTo(IDENTITY_LINK_OPERATION_DELETE);
//               assertThat(userTasks.get(0).getCandidateGroups()).containsExactly("secondGroup");
//             });
//   }
//
//   @Test
//   public void deleteAssigneeAndDeleteCandidateGroupAsLastOperations() throws Exception {
//     // given
//     ProcessInstanceEngineDto engineDto =
//         engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.createGroup("firstGroup");
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("firstGroup");
//     engineIntegrationExtension.deleteCandidateGroupForAllRunningUserTasks("firstGroup");
//     engineIntegrationExtension.claimAllRunningUserTasks();
//     engineIntegrationExtension.unclaimAllRunningUserTasks();
//     engineIntegrationExtension.completeUserTaskWithoutClaim(engineDto.getId());
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               List<FlowNodeInstanceDto> userTasks = processInstanceDto.getUserTasks();
//               assertThat(userTasks).hasSize(1);
//               assertThat(userTasks.get(0).getAssignee()).isNull();
//               assertThat(userTasks.get(0).getCandidateGroups()).isEmpty();
//             });
//   }
//
//   @Test
//   public void importIsNotAffectedByPagination() throws Exception {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.claimAllRunningUserTasks();
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     engineIntegrationExtension.unclaimAllRunningUserTasks();
//     engineIntegrationExtension.addUser("secondUser", "aPassword");
//     engineIntegrationExtension.grantAllAuthorizations("secondUser");
//     engineIntegrationExtension.finishAllRunningUserTasks("secondUser", "aPassword");
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               List<FlowNodeInstanceDto> userTasks = processInstanceDto.getUserTasks();
//               assertThat(userTasks).hasSize(1);
//               assertThat(userTasks.get(0).getAssignee()).isEqualTo("secondUser");
//             });
//   }
//
//   @Test
//   public void onlyUserAssigneeOperationLogsRelatedToProcessInstancesAreImported()
//       throws IOException {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     engineIntegrationExtension.createIndependentUserTask();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
//         .singleElement()
//         .satisfies(
//             processInstanceDto -> {
//               assertThat(processInstanceDto.getUserTasks()).hasSize(1);
//               processInstanceDto
//                   .getUserTasks()
//                   .forEach(userTask -> assertThat(userTask.getAssigneeOperations()).hasSize(1));
//             });
//   }
//
//   @Test
//   public void duplicateUserTasksAreHandledOnUpsert() throws JsonProcessingException {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSingleUserTaskDiagram());
//     String defaultCandidateGroup = "defaultCandidateGroupId";
//     engineIntegrationExtension.createGroup(defaultCandidateGroup);
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(defaultCandidateGroup);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     final ProcessInstanceDto storedInstance = getStoredProcessInstance();
//     assertThat(storedInstance.getUserTasks()).hasSize(1);
//
//     // when duplicate user tasks and tasks with same ID have been stored
//     final FlowNodeInstanceDto userTaskInstanceDto = storedInstance.getUserTasks().get(0);
//
//     final FlowNodeInstanceDto sameIdTaskInstanceDto =
//         new FlowNodeInstanceDto()
//             .setFlowNodeInstanceId(userTaskInstanceDto.getFlowNodeInstanceId())
//             .setFlowNodeType(userTaskInstanceDto.getFlowNodeType())
//             .setUserTaskInstanceId(userTaskInstanceDto.getUserTaskInstanceId());
//     final List<FlowNodeInstanceDto> duplicateTaskList =
//         Arrays.asList(userTaskInstanceDto, userTaskInstanceDto, sameIdTaskInstanceDto);
//     storedInstance.setFlowNodeInstances(duplicateTaskList);
//     databaseIntegrationTestExtension.addEntryToDatabase(
//         getProcessInstanceIndexAliasName(storedInstance.getProcessDefinitionKey()),
//         storedInstance.getProcessInstanceId(),
//         storedInstance);
//
//     final ProcessInstanceDto storedInstanceWithDuplicateUserTasks = getStoredProcessInstance();
//     assertThat(storedInstanceWithDuplicateUserTasks.getUserTasks())
//         .hasSize(3)
//         .containsExactlyElementsOf(duplicateTaskList);
//
//     // and we reimport tasks
//     importAllEngineEntitiesFromScratch();
//
//     // then only single user task with ID remains
//     final ProcessInstanceDto updatedInstance = getStoredProcessInstance();
//     assertThat(updatedInstance.getUserTasks()).hasSize(1);
//   }
//
//   private ProcessInstanceDto getStoredProcessInstance() {
//     final List<ProcessInstanceDto> allProcessInstances =
//         databaseIntegrationTestExtension.getAllProcessInstances();
//     assertThat(allProcessInstances).hasSize(1);
//     return allProcessInstances.get(0);
//   }
// }
