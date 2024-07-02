/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing;

import static io.camunda.optimize.dto.optimize.importing.IdentityLinkLogOperationType.CLAIM_OPERATION_TYPE;
import static io.camunda.optimize.dto.optimize.importing.IdentityLinkLogOperationType.UNCLAIM_OPERATION_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.ZEEBE_USER_TASK_INDEX_NAME;
import static io.camunda.optimize.service.util.importing.EngineConstants.FLOW_NODE_TYPE_USER_TASK;
import static io.camunda.optimize.service.util.importing.ZeebeConstants.ZEEBE_DEFAULT_TENANT_ID;
import static io.camunda.optimize.util.ZeebeBpmnModels.USER_TASK;
import static io.camunda.optimize.util.ZeebeBpmnModels.createSimpleNativeUserTaskProcess;
import static io.camunda.optimize.util.ZeebeBpmnModels.createSimpleNativeUserTaskProcessWithAssignee;
import static io.camunda.optimize.util.ZeebeBpmnModels.createSimpleNativeUserTaskProcessWithCandidateGroup;
import static io.camunda.zeebe.protocol.record.intent.UserTaskIntent.ASSIGNED;
import static io.camunda.zeebe.protocol.record.intent.UserTaskIntent.CANCELED;
import static io.camunda.zeebe.protocol.record.intent.UserTaskIntent.COMPLETED;
import static io.camunda.zeebe.protocol.record.intent.UserTaskIntent.CREATING;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractCCSMIT;
import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.dto.optimize.persistence.AssigneeOperationDto;
import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
import io.camunda.optimize.dto.zeebe.usertask.ZeebeUserTaskDataDto;
import io.camunda.optimize.dto.zeebe.usertask.ZeebeUserTaskRecordDto;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.intent.UserTaskIntent;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

@DisabledIf("isZeebeVersionPre85")
public class ZeebeUserTaskImportIT extends AbstractCCSMIT {

  private static final String TEST_PROCESS = "aProcess";
  private static final String DUE_DATE = "2023-11-01T12:00:00+05:00";
  private static final String ASSIGNEE_ID = "assigneeId";

  @Test
  public void importRunningZeebeUserTaskData() {
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  .singleElement() // only userTask was imported because all other records were
                  // removed
                  .usingRecursiveComparison()
                  .isEqualTo(
                      createRunningUserTaskInstance(instance, exportedEvents)
                          .setDueDate(OffsetDateTime.parse(DUE_DATE)));
            });
  }

  @Test
  public void importCompletedUnclaimedZeebeUserTaskData_viaWriter() {
    // import all data for completed usertask (creation and completion) in one batch, hence the
    // upsert inserts the new instance created with the logic in the writer
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    List<ZeebeUserTaskRecordDto> userTaskEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.completeZeebeUserTask(getExpectedUserTaskInstanceIdFromRecords(userTaskEvents));
    waitUntilUserTaskRecordWithIntentExported(COMPLETED);

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    userTaskEvents = getZeebeExportedUserTaskEvents();
    final OffsetDateTime expectedEndDate =
        getExpectedEndDateForCompletedUserTaskEvents(userTaskEvents);
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, userTaskEvents);
    expectedUserTask
        .setDueDate(OffsetDateTime.parse(DUE_DATE))
        .setEndDate(expectedEndDate)
        .setIdleDurationInMs(0L)
        .setTotalDurationInMs(getExpectedTotalDurationForCompletedUserTask(userTaskEvents))
        .setWorkDurationInMs(getExpectedTotalDurationForCompletedUserTask(userTaskEvents));

    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  .singleElement() // only the userTask was imported because all other records were
                  // removed
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importCompletedUnclaimedZeebeUserTaskData_viaUpdateScript() {
    // import completed userTask data after the first userTask record was already imported, hence
    // the upsert uses the logic from the update script
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));

    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    importAllZeebeEntitiesFromScratch();
    final List<ZeebeUserTaskRecordDto> runningUserTaskEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, runningUserTaskEvents);
    List<ZeebeUserTaskRecordDto> userTaskEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.completeZeebeUserTask(getExpectedUserTaskInstanceIdFromRecords(userTaskEvents));
    waitUntilUserTaskRecordWithIntentExported(COMPLETED);

    // when
    importAllZeebeEntitiesFromLastIndex();

    // then
    userTaskEvents = getZeebeExportedUserTaskEvents();
    final OffsetDateTime expectedEndDate =
        getExpectedEndDateForCompletedUserTaskEvents(userTaskEvents);
    expectedUserTask
        .setDueDate(OffsetDateTime.parse(DUE_DATE))
        .setEndDate(expectedEndDate)
        .setIdleDurationInMs(0L)
        .setTotalDurationInMs(getExpectedTotalDurationForCompletedUserTask(userTaskEvents))
        .setWorkDurationInMs(getExpectedTotalDurationForCompletedUserTask(userTaskEvents));

    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  .singleElement() // only the userTask was imported because all other records were
                  // removed
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importCanceledUnclaimedZeebeUserTaskData_viaWriter() {
    // import all data for canceled usertask (creation and cancellation) in one batch, hence the
    // upsert inserts the new instance
    // created with the logic in the writer
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    zeebeExtension.cancelProcessInstance(instance.getProcessInstanceKey());
    waitUntilUserTaskRecordWithIntentExported(UserTaskIntent.CANCELED);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then the import in one batch correctly set all fields in the new instance document
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents);
    final OffsetDateTime expectedEndDate =
        getExpectedEndDateForCanceledUserTaskEvents(exportedEvents);
    final Long expectedTotalAndIdleDuration =
        Duration.between(expectedUserTask.getStartDate(), expectedEndDate).toMillis();
    expectedUserTask
        .setDueDate(OffsetDateTime.parse(DUE_DATE))
        .setEndDate(expectedEndDate)
        .setTotalDurationInMs(expectedTotalAndIdleDuration)
        .setIdleDurationInMs(expectedTotalAndIdleDuration)
        .setWorkDurationInMs(0L)
        .setCanceled(true);

    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  .singleElement() // only userTask was imported because all other records were
                  // removed
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importCanceledUnclaimedZeebeUserTaskData_viaUpdateScript() {
    // import canceled userTask data after the first userTask record was already imported, hence the
    // upsert uses the logic  from the update script
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    importAllZeebeEntitiesFromScratch();
    zeebeExtension.cancelProcessInstance(instance.getProcessInstanceKey());
    waitUntilUserTaskRecordWithIntentExported(UserTaskIntent.CANCELED);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromLastIndex();

    // then the import over two batches correctly updates all fields with the update script
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents);
    final OffsetDateTime expectedEndDate =
        getExpectedEndDateForCanceledUserTaskEvents(exportedEvents);
    expectedUserTask
        .setDueDate(OffsetDateTime.parse(DUE_DATE))
        .setEndDate(expectedEndDate)
        .setTotalDurationInMs(
            Duration.between(expectedUserTask.getStartDate(), expectedEndDate).toMillis())
        .setIdleDurationInMs(
            Duration.between(expectedUserTask.getStartDate(), expectedEndDate).toMillis())
        .setWorkDurationInMs(0L)
        .setCanceled(true);

    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importCanceledClaimedZeebeUserTaskData_viaWriter() {
    // import all data for canceled usertask in one batch, hence the upsert inserts the new instance
    // created with the logic in the writer
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.assignUserTask(
        getExpectedUserTaskInstanceIdFromRecords(exportedEvents), ASSIGNEE_ID);
    zeebeExtension.cancelProcessInstance(instance.getProcessInstanceKey());
    waitUntilUserTaskRecordWithIntentExported(CANCELED);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    exportedEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents);
    final OffsetDateTime expectedEndDate =
        getExpectedEndDateForCanceledUserTaskEvents(exportedEvents);
    final OffsetDateTime assignDate = getTimestampForAssignedUserTaskEvents(exportedEvents);
    expectedUserTask
        .setAssignee(ASSIGNEE_ID)
        .setDueDate(OffsetDateTime.parse(DUE_DATE))
        .setEndDate(expectedEndDate)
        .setTotalDurationInMs(
            Duration.between(expectedUserTask.getStartDate(), expectedEndDate).toMillis())
        .setIdleDurationInMs(
            Duration.between(expectedUserTask.getStartDate(), assignDate).toMillis())
        .setWorkDurationInMs(Duration.between(assignDate, expectedEndDate).toMillis())
        .setAssigneeOperations(
            List.of(
                new AssigneeOperationDto()
                    .setId(getExpectedIdFromRecords(exportedEvents, ASSIGNED))
                    .setOperationType(CLAIM_OPERATION_TYPE.toString())
                    .setUserId(ASSIGNEE_ID)
                    .setTimestamp(getTimestampForAssignedUserTaskEvents(exportedEvents))))
        .setCanceled(true);

    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importCanceledClaimedZeebeUserTaskData_viaUpdateScript() {
    // import canceled userTask data after the first userTask records were already imported, hence
    // the upsert uses the logic from the update script
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, DUE_DATE));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.assignUserTask(
        getExpectedUserTaskInstanceIdFromRecords(exportedEvents), ASSIGNEE_ID);
    waitUntilUserTaskRecordWithIntentExported(ASSIGNED);

    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    importAllZeebeEntitiesFromScratch();

    zeebeExtension.cancelProcessInstance(instance.getProcessInstanceKey());
    waitUntilUserTaskRecordWithIntentExported(UserTaskIntent.CANCELED);
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromLastIndex();

    // then
    exportedEvents = getZeebeExportedUserTaskEvents();
    final OffsetDateTime expectedEndDate =
        getExpectedEndDateForCanceledUserTaskEvents(exportedEvents);
    final OffsetDateTime assignDate = getTimestampForAssignedUserTaskEvents(exportedEvents);
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents);
    expectedUserTask
        .setAssignee(ASSIGNEE_ID)
        .setDueDate(OffsetDateTime.parse(DUE_DATE))
        .setEndDate(expectedEndDate)
        .setTotalDurationInMs(
            Duration.between(expectedUserTask.getStartDate(), expectedEndDate).toMillis())
        .setIdleDurationInMs(
            Duration.between(expectedUserTask.getStartDate(), assignDate).toMillis())
        .setWorkDurationInMs(Duration.between(assignDate, expectedEndDate).toMillis())
        .setAssigneeOperations(
            List.of(
                new AssigneeOperationDto()
                    .setId(getExpectedIdFromRecords(exportedEvents, ASSIGNED))
                    .setOperationType(CLAIM_OPERATION_TYPE.toString())
                    .setUserId(ASSIGNEE_ID)
                    .setTimestamp(getTimestampForAssignedUserTaskEvents(exportedEvents))))
        .setCanceled(true);

    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importClaimOperation_viaWriter() {
    // import assignee usertask operations in one batch, hence the upsert inserts the new instance
    // created with the logic in the writer
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, null));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.assignUserTask(
        getExpectedUserTaskInstanceIdFromRecords(exportedEvents), ASSIGNEE_ID);
    waitUntilUserTaskRecordWithIntentExported(ASSIGNED);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    exportedEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents)
            .setIdleDurationInMs(getDurationInMsBetweenStartAndFirstAssignOperation(exportedEvents))
            .setAssignee(ASSIGNEE_ID)
            .setAssigneeOperations(
                List.of(
                    new AssigneeOperationDto()
                        .setId(getExpectedIdFromRecords(exportedEvents, ASSIGNED))
                        .setUserId(ASSIGNEE_ID)
                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                        .setTimestamp(
                            getTimestampForZeebeAssignEvents(exportedEvents, ASSIGNEE_ID))));
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importClaimOperation_viaUpdateScript() {
    // import assignee userTask data after the first userTask records were already imported, hence
    // the upsert uses the logic from the update script
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, null));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    importAllZeebeEntitiesFromScratch();

    List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.assignUserTask(
        getExpectedUserTaskInstanceIdFromRecords(exportedEvents), ASSIGNEE_ID);
    waitUntilUserTaskRecordWithIntentExported(ASSIGNED);

    // when
    importAllZeebeEntitiesFromLastIndex();

    // then
    exportedEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents)
            .setIdleDurationInMs(getDurationInMsBetweenStartAndFirstAssignOperation(exportedEvents))
            .setAssignee(ASSIGNEE_ID)
            .setAssigneeOperations(
                List.of(
                    new AssigneeOperationDto()
                        .setId(getExpectedIdFromRecords(exportedEvents, ASSIGNED))
                        .setUserId(ASSIGNEE_ID)
                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                        .setTimestamp(
                            getTimestampForZeebeAssignEvents(exportedEvents, ASSIGNEE_ID))));
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importUnclaimOperation_viaWriter() {
    // import assignee usertask operations in one batch, hence the upsert inserts the new instance
    // created with the logic in the writer
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(
            createSimpleNativeUserTaskProcessWithAssignee(TEST_PROCESS, null, ASSIGNEE_ID));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    zeebeExtension.unassignUserTask(
        getExpectedUserTaskInstanceIdFromRecords(getZeebeExportedUserTaskEvents()));
    waitUntilUserTaskRecordWithIntentExported(ASSIGNED);

    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(
                      createRunningUserTaskInstance(instance, exportedEvents)
                          .setIdleDurationInMs(0L)
                          .setWorkDurationInMs(
                              getDurationInMsBetweenStartAndFirstAssignOperation(exportedEvents))
                          .setAssigneeOperations(
                              List.of(
                                  new AssigneeOperationDto()
                                      .setId(getExpectedIdFromRecords(exportedEvents, CREATING))
                                      .setUserId(ASSIGNEE_ID)
                                      .setOperationType(CLAIM_OPERATION_TYPE.toString())
                                      .setTimestamp(
                                          getExpectedStartDateForUserTaskEvents(exportedEvents)),
                                  new AssigneeOperationDto()
                                      .setId(getExpectedIdFromRecords(exportedEvents, ASSIGNED))
                                      .setOperationType(UNCLAIM_OPERATION_TYPE.toString())
                                      .setTimestamp(
                                          getTimestampForAssignedUserTaskEvents(exportedEvents)))));
            });
  }

  @Test
  public void importUnclaimOperation_viaUpdateScript() {
    // import assignee userTask data after the first userTask records were already imported, hence
    // the upsert uses the logic from the update script
    // given
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(
            createSimpleNativeUserTaskProcessWithAssignee(TEST_PROCESS, null, ASSIGNEE_ID));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    importAllZeebeEntitiesFromScratch();

    List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.unassignUserTask(getExpectedUserTaskInstanceIdFromRecords(exportedEvents));
    waitUntilUserTaskRecordWithIntentExported(ASSIGNED);

    // when
    importAllZeebeEntitiesFromLastIndex();

    // then
    exportedEvents = getZeebeExportedUserTaskEvents();
    final FlowNodeInstanceDto expectedUserTask =
        createRunningUserTaskInstance(instance, exportedEvents)
            .setIdleDurationInMs(0L)
            .setWorkDurationInMs(getDurationInMsBetweenStartAndFirstAssignOperation(exportedEvents))
            .setAssigneeOperations(
                List.of(
                    new AssigneeOperationDto()
                        .setId(getExpectedIdFromRecords(exportedEvents, CREATING))
                        .setUserId(ASSIGNEE_ID)
                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                        .setTimestamp(getExpectedStartDateForUserTaskEvents(exportedEvents)),
                    new AssigneeOperationDto()
                        .setId(getExpectedIdFromRecords(exportedEvents, ASSIGNED))
                        .setOperationType(UNCLAIM_OPERATION_TYPE.toString())
                        .setTimestamp(getTimestampForZeebeUnassignEvent(exportedEvents))));
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(expectedUserTask);
            });
  }

  @Test
  public void importAssignee_fromCreationRecord() {
    // given a process that was started with an assignee already present in the model
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(
            createSimpleNativeUserTaskProcessWithAssignee(TEST_PROCESS, DUE_DATE, ASSIGNEE_ID));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance -> {
              assertThat(savedInstance.getProcessInstanceId())
                  .isEqualTo(String.valueOf(instance.getProcessInstanceKey()));
              assertThat(savedInstance.getProcessDefinitionId())
                  .isEqualTo(String.valueOf(instance.getProcessDefinitionKey()));
              assertThat(savedInstance.getProcessDefinitionKey())
                  .isEqualTo(instance.getBpmnProcessId());
              assertThat(savedInstance.getDataSource().getName())
                  .isEqualTo(getConfiguredZeebeName());
              assertThat(savedInstance.getTenantId()).isEqualTo(ZEEBE_DEFAULT_TENANT_ID);
              assertThat(savedInstance.getFlowNodeInstances())
                  // only userTask was imported because all other records were removed
                  .singleElement()
                  .usingRecursiveComparison()
                  .isEqualTo(
                      createRunningUserTaskInstance(instance, exportedEvents)
                          .setDueDate(OffsetDateTime.parse(DUE_DATE))
                          .setIdleDurationInMs(0L)
                          .setAssignee(ASSIGNEE_ID)
                          .setAssigneeOperations(
                              List.of(
                                  new AssigneeOperationDto()
                                      .setId(getExpectedIdFromRecords(exportedEvents, CREATING))
                                      .setUserId(ASSIGNEE_ID)
                                      .setOperationType(CLAIM_OPERATION_TYPE.toString())
                                      .setTimestamp(
                                          getExpectedStartDateForUserTaskEvents(exportedEvents)))));
            });
  }

  @Test
  public void importMultipleAssigneeOperations_viaWriter() {
    // given
    final String assigneeId1 = ASSIGNEE_ID + "1";
    final String assigneeId2 = ASSIGNEE_ID + "2";
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, null));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    final List<ZeebeUserTaskRecordDto> userTaskEvents = getZeebeExportedUserTaskEvents();
    final long userTaskInstanceId = getExpectedUserTaskInstanceIdFromRecords(userTaskEvents);
    zeebeExtension.assignUserTask(userTaskInstanceId, assigneeId1);
    zeebeExtension.unassignUserTask(userTaskInstanceId);
    zeebeExtension.assignUserTask(userTaskInstanceId, assigneeId2);
    zeebeExtension.completeZeebeUserTask(userTaskInstanceId);
    waitUntilUserTaskRecordWithIntentExported(COMPLETED);

    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromScratch();

    // then
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance ->
                assertThat(savedInstance.getFlowNodeInstances())
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(
                        createRunningUserTaskInstance(instance, exportedEvents)
                            .setEndDate(
                                getExpectedEndDateForCompletedUserTaskEvents(exportedEvents))
                            .setIdleDurationInMs(
                                getDurationInMsBetweenStartAndFirstAssignOperation(exportedEvents)
                                    + getDurationInMsBetweenAssignOperations(
                                        exportedEvents, "", assigneeId2))
                            .setWorkDurationInMs(
                                getDurationInMsBetweenAssignOperations(
                                        exportedEvents, assigneeId1, "")
                                    + getDurationInMsBetweenLastAssignOperationAndEnd(
                                        exportedEvents, assigneeId2))
                            .setTotalDurationInMs(
                                getExpectedTotalDurationForCompletedUserTask(exportedEvents))
                            .setAssignee(assigneeId2)
                            .setAssigneeOperations(
                                List.of(
                                    new AssigneeOperationDto()
                                        .setId(
                                            getExpectedIdFromAssignRecordsWithAssigneeId(
                                                exportedEvents, assigneeId1))
                                        .setUserId(assigneeId1)
                                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                                        .setTimestamp(
                                            getTimestampForZeebeAssignEvents(
                                                exportedEvents, assigneeId1)),
                                    new AssigneeOperationDto()
                                        .setId(
                                            getExpectedIdFromAssignRecordsWithAssigneeId(
                                                exportedEvents, ""))
                                        .setOperationType(UNCLAIM_OPERATION_TYPE.toString())
                                        .setTimestamp(
                                            getTimestampForZeebeUnassignEvent(exportedEvents)),
                                    new AssigneeOperationDto()
                                        .setId(
                                            getExpectedIdFromAssignRecordsWithAssigneeId(
                                                exportedEvents, assigneeId2))
                                        .setUserId(assigneeId2)
                                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                                        .setTimestamp(
                                            getTimestampForZeebeAssignEvents(
                                                exportedEvents, assigneeId2))))));
  }

  @Test
  public void importMultipleAssigneeOperations_viaUpdateScript() {
    // given
    final String assigneeId1 = ASSIGNEE_ID + "1";
    final String assigneeId2 = ASSIGNEE_ID + "2";
    final ProcessInstanceEvent instance =
        deployAndStartInstanceForProcess(createSimpleNativeUserTaskProcess(TEST_PROCESS, null));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    final List<ZeebeUserTaskRecordDto> userTaskEvents = getZeebeExportedUserTaskEvents();
    final long userTaskInstanceId = getExpectedUserTaskInstanceIdFromRecords(userTaskEvents);
    zeebeExtension.assignUserTask(userTaskInstanceId, assigneeId1);
    waitUntilUserTaskRecordWithIntentExported(ASSIGNED);
    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();
    importAllZeebeEntitiesFromScratch();

    zeebeExtension.unassignUserTask(userTaskInstanceId);
    zeebeExtension.assignUserTask(userTaskInstanceId, assigneeId2);
    zeebeExtension.completeZeebeUserTask(userTaskInstanceId);
    waitUntilUserTaskRecordWithIntentExported(COMPLETED);

    // remove all zeebe records except userTask ones to test userTask import only
    removeAllZeebeExportRecordsExceptUserTaskRecords();

    // when
    importAllZeebeEntitiesFromLastIndex();

    // then
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .singleElement()
        .satisfies(
            savedInstance ->
                assertThat(savedInstance.getFlowNodeInstances())
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(
                        createRunningUserTaskInstance(instance, exportedEvents)
                            .setEndDate(
                                getExpectedEndDateForCompletedUserTaskEvents(exportedEvents))
                            .setIdleDurationInMs(
                                getDurationInMsBetweenStartAndFirstAssignOperation(exportedEvents)
                                    + getDurationInMsBetweenAssignOperations(
                                        exportedEvents, "", assigneeId2))
                            .setWorkDurationInMs(
                                getDurationInMsBetweenAssignOperations(
                                        exportedEvents, assigneeId1, "")
                                    + getDurationInMsBetweenLastAssignOperationAndEnd(
                                        exportedEvents, assigneeId2))
                            .setTotalDurationInMs(
                                getExpectedTotalDurationForCompletedUserTask(exportedEvents))
                            .setAssignee(assigneeId2)
                            .setAssigneeOperations(
                                List.of(
                                    new AssigneeOperationDto()
                                        .setId(
                                            getExpectedIdFromAssignRecordsWithAssigneeId(
                                                exportedEvents, assigneeId1))
                                        .setUserId(assigneeId1)
                                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                                        .setTimestamp(
                                            getTimestampForZeebeAssignEvents(
                                                exportedEvents, assigneeId1)),
                                    new AssigneeOperationDto()
                                        .setId(
                                            getExpectedIdFromAssignRecordsWithAssigneeId(
                                                exportedEvents, ""))
                                        .setOperationType(UNCLAIM_OPERATION_TYPE.toString())
                                        .setTimestamp(
                                            getTimestampForZeebeUnassignEvent(exportedEvents)),
                                    new AssigneeOperationDto()
                                        .setId(
                                            getExpectedIdFromAssignRecordsWithAssigneeId(
                                                exportedEvents, assigneeId2))
                                        .setUserId(assigneeId2)
                                        .setOperationType(CLAIM_OPERATION_TYPE.toString())
                                        .setTimestamp(
                                            getTimestampForZeebeAssignEvents(
                                                exportedEvents, assigneeId2))))));
  }

  @Test
  public void doNotImportCandidateGroupUpdates() {
    // given
    deployAndStartInstanceForProcess(
        createSimpleNativeUserTaskProcessWithCandidateGroup(
            TEST_PROCESS, DUE_DATE, "aCandidateGroup"));
    waitUntilUserTaskRecordWithElementIdExported(USER_TASK);
    final List<ZeebeUserTaskRecordDto> exportedEvents = getZeebeExportedUserTaskEvents();
    zeebeExtension.updateCandidateGroupForUserTask(
        getExpectedUserTaskInstanceIdFromRecords(exportedEvents), "anotherCandidateGroup");

    // when
    importAllZeebeEntitiesFromScratch();

    // then no candidate group data was imported
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances())
        .flatExtracting(ProcessInstanceDto::getFlowNodeInstances)
        .extracting(FlowNodeInstanceDto::getCandidateGroups)
        .containsOnly(Collections.emptyList());
  }

  private FlowNodeInstanceDto createRunningUserTaskInstance(
      final ProcessInstanceEvent deployedInstance, final List<ZeebeUserTaskRecordDto> events) {
    return new FlowNodeInstanceDto()
        .setFlowNodeInstanceId(String.valueOf(events.get(0).getValue().getElementInstanceKey()))
        .setFlowNodeId(USER_TASK)
        .setFlowNodeType(FLOW_NODE_TYPE_USER_TASK)
        .setProcessInstanceId(String.valueOf(deployedInstance.getProcessInstanceKey()))
        .setDefinitionKey(String.valueOf(deployedInstance.getBpmnProcessId()))
        .setDefinitionVersion(String.valueOf(deployedInstance.getVersion()))
        .setTenantId(ZEEBE_DEFAULT_TENANT_ID)
        .setUserTaskInstanceId(String.valueOf(getExpectedUserTaskInstanceIdFromRecords(events)))
        .setStartDate(getExpectedStartDateForUserTaskEvents(events))
        .setCanceled(false);
  }

  private OffsetDateTime getExpectedStartDateForUserTaskEvents(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return getTimestampForFirstZeebeEventsWithIntent(eventsForElement, UserTaskIntent.CREATING);
  }

  private OffsetDateTime getExpectedEndDateForCompletedUserTaskEvents(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return getTimestampForFirstZeebeEventsWithIntent(eventsForElement, COMPLETED);
  }

  private OffsetDateTime getTimestampForAssignedUserTaskEvents(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return getTimestampForFirstZeebeEventsWithIntent(eventsForElement, ASSIGNED);
  }

  private OffsetDateTime getExpectedEndDateForCanceledUserTaskEvents(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return getTimestampForFirstZeebeEventsWithIntent(eventsForElement, UserTaskIntent.CANCELED);
  }

  private long getExpectedTotalDurationForCompletedUserTask(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return Duration.between(
            getExpectedStartDateForUserTaskEvents(eventsForElement),
            getExpectedEndDateForCompletedUserTaskEvents(eventsForElement))
        .toMillis();
  }

  private long getDurationInMsBetweenStartAndFirstAssignOperation(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return Duration.between(
            getExpectedStartDateForUserTaskEvents(eventsForElement),
            getTimestampForAssignedUserTaskEvents(eventsForElement))
        .toMillis();
  }

  private long getDurationInMsBetweenAssignOperations(
      final List<ZeebeUserTaskRecordDto> eventsForElement,
      final String assigneeId1,
      final String assigneeId2) {
    return Duration.between(
            getTimestampForZeebeAssignEvents(eventsForElement, assigneeId1),
            getTimestampForZeebeAssignEvents(eventsForElement, assigneeId2))
        .toMillis();
  }

  private long getDurationInMsBetweenLastAssignOperationAndEnd(
      final List<ZeebeUserTaskRecordDto> eventsForElement, final String assigneeId) {
    return Duration.between(
            getTimestampForZeebeAssignEvents(eventsForElement, assigneeId),
            getExpectedEndDateForCompletedUserTaskEvents(eventsForElement))
        .toMillis();
  }

  private long getExpectedUserTaskInstanceIdFromRecords(
      final List<ZeebeUserTaskRecordDto> eventsForElement) {
    return eventsForElement.stream()
        .findFirst()
        .map(ZeebeUserTaskRecordDto::getValue)
        .map(ZeebeUserTaskDataDto::getUserTaskKey)
        .orElseThrow(eventNotFoundExceptionSupplier);
  }

  private String getExpectedIdFromRecords(
      final List<ZeebeUserTaskRecordDto> eventsForElement, final UserTaskIntent intent) {
    return eventsForElement.stream()
        .filter(event -> intent.equals(event.getIntent()))
        .findFirst()
        .map(ZeebeUserTaskRecordDto::getKey)
        .map(String::valueOf)
        .orElseThrow(eventNotFoundExceptionSupplier);
  }

  private String getExpectedIdFromAssignRecordsWithAssigneeId(
      final List<ZeebeUserTaskRecordDto> eventsForElement, final String assigneeId) {
    return eventsForElement.stream()
        .filter(
            event ->
                ASSIGNED.equals(event.getIntent())
                    && assigneeId.equals(event.getValue().getAssignee()))
        .findFirst()
        .map(ZeebeUserTaskRecordDto::getKey)
        .map(String::valueOf)
        .orElseThrow(eventNotFoundExceptionSupplier);
  }

  private void removeAllZeebeExportRecordsExceptUserTaskRecords() {
    databaseIntegrationTestExtension.deleteAllOtherZeebeRecordsWithPrefix(
        zeebeExtension.getZeebeRecordPrefix(), ZEEBE_USER_TASK_INDEX_NAME);
  }

  private List<ZeebeUserTaskRecordDto> getZeebeExportedUserTaskEvents() {
    return getZeebeExportedUserTaskEventsByElementId().get(USER_TASK);
  }
}
