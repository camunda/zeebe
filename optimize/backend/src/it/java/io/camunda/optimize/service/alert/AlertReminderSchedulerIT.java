/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.alert;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractAlertIT;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.alert.AlertCreationRequestDto;
import io.camunda.optimize.dto.optimize.query.alert.AlertInterval;
import io.camunda.optimize.dto.optimize.query.alert.AlertIntervalUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessVisualization;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.FlowNodesGroupByDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

public class AlertReminderSchedulerIT extends AbstractAlertIT {

  @Test
  public void reminderJobsAreRemovedOnAlertDeletion() throws Exception {
    // given
    AlertCreationRequestDto simpleAlert = createBasicAlertWithReminder();
    String id = alertClient.createAlert(simpleAlert);
    triggerAndCompleteCheckJob(id);

    // when
    alertClient.deleteAlert(id);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(0);
  }

  @Test
  public void reminderJobsAreRemovedOnReportDeletion() throws Exception {
    // given
    AlertCreationRequestDto simpleAlert = createBasicAlertWithReminder();
    String id = alertClient.createAlert(simpleAlert);
    triggerAndCompleteCheckJob(id);

    // when
    reportClient.deleteReport(simpleAlert.getReportId(), true);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(0);
  }

  @Test
  public void reminderIsNotCreatedOnStartupIfNotDefinedInAlert() {
    // given
    AlertCreationRequestDto alert = setupBasicProcessAlert();
    alertClient.createAlert(alert);

    // when
    startAndUseNewOptimizeInstance();

    // then
    assertThat(alertClient.getAllAlerts().get(0).getReminder()).isNull();
  }

  @Test
  public void reminderJobsAreRemovedOnReportUpdate() throws Exception {
    ProcessDefinitionEngineDto processDefinition = deployAndStartSimpleServiceTaskProcess();
    importAllEngineEntitiesFromScratch();

    String collectionId = collectionClient.createNewCollectionWithProcessScope(processDefinition);
    String reportId = createNewProcessReportAsUser(collectionId, processDefinition);
    AlertCreationRequestDto simpleAlert = alertClient.createSimpleAlert(reportId);

    AlertInterval reminderInterval = new AlertInterval();
    reminderInterval.setValue(1);
    reminderInterval.setUnit(AlertIntervalUnit.SECONDS);
    simpleAlert.setReminder(reminderInterval);

    String id = alertClient.createAlert(simpleAlert);

    triggerAndCompleteCheckJob(id);
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(2);

    // when
    SingleProcessReportDefinitionRequestDto report =
        getProcessNumberReportDefinitionDto(collectionId, processDefinition);
    report.getData().setGroupBy(new FlowNodesGroupByDto());
    report.getData().setVisualization(ProcessVisualization.HEAT);
    reportClient.updateSingleProcessReport(simpleAlert.getReportId(), report, true);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(0);
  }

  @Test
  public void reminderJobsAreRemovedOnEvaluationResultChange() throws Exception {
    // given
    long daysToShift = 0L;
    long durationInSec = 2L;

    ProcessInstanceEngineDto processInstance = deployWithTimeShift(daysToShift, durationInSec);

    // when
    String reportId = createAndStoreDurationNumberReportInNewCollection(processInstance);
    AlertCreationRequestDto simpleAlert =
        alertClient.createSimpleAlert(reportId, 10, AlertIntervalUnit.SECONDS);
    AlertInterval reminderInterval = new AlertInterval();
    reminderInterval.setValue(1);
    reminderInterval.setUnit(AlertIntervalUnit.MINUTES);
    simpleAlert.setReminder(reminderInterval);

    simpleAlert.setThreshold(1500.0);

    String id = alertClient.createAlert(simpleAlert);

    triggerAndCompleteCheckJob(id);
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSizeGreaterThanOrEqualTo(2);

    // when
    engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());
    importAllEngineEntitiesFromScratch();

    triggerAndCompleteReminderJob(id);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(1);
  }

  @Test
  public void reminderJobsAreRemovedOnAlertDefinitionChange() throws Exception {
    // given
    AlertCreationRequestDto simpleAlert = createBasicAlertWithReminder();

    String id = alertClient.createAlert(simpleAlert);

    triggerAndCompleteCheckJob(id);
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSizeGreaterThanOrEqualTo(2);

    // when
    simpleAlert.getCheckInterval().setValue(30);

    alertClient.updateAlert(id, simpleAlert);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(1);

    // when
    triggerAndCompleteCheckJob(id);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(2);
  }

  @Test
  public void reminderJobsAreScheduledOnAlertCreation() throws Exception {
    // given
    AlertCreationRequestDto simpleAlert = createBasicAlertWithReminder();

    // when
    String id = alertClient.createAlert(simpleAlert);

    triggerAndCompleteCheckJob(id);

    // then
    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(2);
  }

  @Test
  public void reminderJobsAreScheduledCorrectly() throws Exception {
    // given
    AlertCreationRequestDto simpleAlert = createBasicAlertWithReminder();
    AlertInterval checkInterval = new AlertInterval();
    checkInterval.setUnit(AlertIntervalUnit.MINUTES);
    checkInterval.setValue(10);
    simpleAlert.setCheckInterval(checkInterval);

    // when
    String id = alertClient.createAlert(simpleAlert);

    triggerAndCompleteCheckJob(id);

    // then
    OffsetDateTime nextTimeReminderIsExecuted = getNextReminderExecutionTime(id);
    OffsetDateTime upperBoundary = OffsetDateTime.now().plusSeconds(2L); // 1 second is too unstable
    assertThat(nextTimeReminderIsExecuted).isBefore(upperBoundary);
  }

  @Test
  @DirtiesContext
  public void reminderJobsAreScheduledAfterRestart() throws Exception {
    // given
    startAndUseNewOptimizeInstance();
    AlertCreationRequestDto simpleAlert = createBasicAlertWithReminder();

    String id = alertClient.createAlert(simpleAlert);

    triggerAndCompleteCheckJob(id);

    // when
    startAndUseNewOptimizeInstance();

    assertThat(embeddedOptimizeExtension.getAlertService().getScheduler().getJobGroupNames())
        .hasSize(2);
  }
}
