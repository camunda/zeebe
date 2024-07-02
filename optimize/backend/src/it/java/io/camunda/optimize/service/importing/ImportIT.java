/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.ProcessInstanceConstants;
import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.importing.job.VariableUpdateDatabaseImportJob;
import io.camunda.optimize.util.BpmnModels;
import io.github.netmikey.logunit.api.LogCapturer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.LoggingEvent;

@Tag(OPENSEARCH_PASSING)
public class ImportIT extends AbstractImportIT {

  private static final String START_EVENT = "startEvent";
  private static final String USER_TASK_1 = "userTask1";
  private static final String PROC_DEF_KEY = "aProcess";

  @RegisterExtension
  @Order(5)
  protected final LogCapturer logCapturer =
      LogCapturer.create().captureForType(VariableUpdateDatabaseImportJob.class);

  private int originalNestedDocLimit;

  @BeforeEach
  public void setup() {
    originalNestedDocLimit =
        databaseIntegrationTestExtension.getNestedDocumentLimit(
            embeddedOptimizeExtension.getConfigurationService());
  }

  @AfterEach
  public void tearDown() {
    updateProcessInstanceNestedDocLimit(PROC_DEF_KEY, originalNestedDocLimit);
  }

  @Test
  public void nestedDocsLimitExceptionLogIncludesConfigHint() {
    // given a process instance with more nested docs than the limit
    embeddedOptimizeExtension
        .getConfigurationService()
        .setSkipDataAfterNestedDocLimitReached(false);
    final Map<String, Object> variables = new HashMap<>();
    variables.put("var1", 1);
    final ProcessInstanceEngineDto instance =
        deployAndStartSimpleTwoUserTaskProcessWithVariables(variables);
    // import first instance to create the index
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();

    // update index setting and create second instance with more nested docs than the limit
    updateProcessInstanceNestedDocLimit(instance.getProcessDefinitionKey(), 1);
    variables.put("var2", 2);
    engineIntegrationExtension.startProcessInstance(instance.getDefinitionId(), variables);

    // when
    embeddedOptimizeExtension.startContinuousImportScheduling();
    Awaitility.dontCatchUncaughtExceptions()
        .timeout(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              final String regex =
                  "If you are experiencing failures due to too many nested documents, try carefully increasing the configured nested object limit \\((es|opensearch)\\.settings\\.index\\.nested_documents_limit\\) or enabling the skipping of documents that have reached this limit during import \\(import\\.skipDataAfterNestedDocLimitReached\\)\\. See Optimize documentation for details\\.";
              final Pattern pattern = Pattern.compile(regex);
              assertThat(logCapturer.getEvents())
                  .extracting(LoggingEvent::getThrowable)
                  .extracting(Throwable::getMessage)
                  .isNotEmpty()
                  .anyMatch(msg -> pattern.matcher(msg).find());
            });
  }

  @Test
  public void documentsHittingNestedDocLimitAreSkippedOnImportIfConfigurationEnabled() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setSkipDataAfterNestedDocLimitReached(true);
    final ProcessInstanceEngineDto firstInstance = deployAndStartSimpleTwoUserTaskProcess();
    // import instance to create the index
    importAllEngineEntitiesFromScratch();
    // get the current nested document count for first instance
    final ProcessInstanceDto firstInstanceOnFirstRoundImport =
        getProcessInstanceForId(firstInstance.getId());
    final int currentNestedDocCount =
        getNestedDocumentCountForProcessInstance(firstInstanceOnFirstRoundImport);
    // the instance is incomplete so is initially active
    assertThat(firstInstanceOnFirstRoundImport.getState())
        .isEqualTo(ProcessInstanceConstants.ACTIVE_STATE);

    assertThat(currentNestedDocCount).isGreaterThan(0);
    // update index setting so no more nested documents can be stored
    updateProcessInstanceNestedDocLimit(
        firstInstance.getProcessDefinitionKey(), currentNestedDocCount);
    // finished both user tasks so we would expect a second user task and end event flow node
    // instances on next import
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.finishAllRunningUserTasks();
    // and start a second instance, which should still be imported
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(firstInstance.getDefinitionId());

    // when
    importAllEngineEntitiesFromLastIndex();

    // then the first instance does not get updated with new nested data
    final ProcessInstanceDto firstInstanceAfterSecondRoundImport =
        getProcessInstanceForId(firstInstance.getId());
    assertThat(firstInstanceAfterSecondRoundImport.getFlowNodeInstances())
        .isEqualTo(firstInstanceOnFirstRoundImport.getFlowNodeInstances());
    // but the parent document state can still be updated
    assertThat(firstInstanceAfterSecondRoundImport.getState())
        .isEqualTo(ProcessInstanceConstants.COMPLETED_STATE);
    // and the second instance can be imported included its nested document
    assertThat(getProcessInstanceForId(secondInstance.getId()).getFlowNodeInstances())
        .extracting(FlowNodeInstanceDto::getFlowNodeId)
        .containsExactlyInAnyOrder(START_EVENT, USER_TASK_1);
  }

  @Test
  public void importIsNotBlockedIfDefinitionDeletedAndNotImported() {
    // given a definition that was deleted in engine before it could be imported, but instance data
    // remains in engine
    embeddedOptimizeExtension
        .getConfigurationService()
        .getConfiguredEngines()
        .values()
        .forEach(engineConfiguration -> engineConfiguration.setImportEnabled(false));
    embeddedOptimizeExtension.reloadConfiguration();

    final ProcessInstanceEngineDto instanceFromDeletedProcess =
        engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram(PROC_DEF_KEY));

    // relevant data that could block import: flownodes, userOperationsLog, incidents
    engineIntegrationExtension.suspendProcessInstanceByInstanceId(
        instanceFromDeletedProcess.getId());
    engineIntegrationExtension.unsuspendProcessInstanceByInstanceId(
        instanceFromDeletedProcess.getId());
    incidentClient.createOpenIncidentForInstancesWithBusinessKey(
        instanceFromDeletedProcess.getBusinessKey());
    // need to complete usertasks because definitions with running instances cannot be deleted
    // without cascade
    engineIntegrationExtension.completeUserTaskWithoutClaim(instanceFromDeletedProcess.getId());
    engineIntegrationExtension.deleteProcessDefinition(
        instanceFromDeletedProcess.getDefinitionId());

    // when
    importAllEngineEntitiesFromScratch();

    // then no exceptions occur during import
    assertThat(databaseIntegrationTestExtension.getAllProcessInstances()).hasSize(1);
  }

  private int getNestedDocumentCountForProcessInstance(final ProcessInstanceDto instance) {
    return instance.getFlowNodeInstances().size()
        + instance.getVariables().size()
        + instance.getIncidents().size();
  }

  private ProcessInstanceDto getProcessInstanceForId(final String processInstanceId) {
    final List<ProcessInstanceDto> instances =
        databaseIntegrationTestExtension.getAllProcessInstances().stream()
            .filter(instance -> instance.getProcessInstanceId().equals(processInstanceId))
            .collect(Collectors.toList());
    assertThat(instances).hasSize(1);
    return instances.get(0);
  }

  @SneakyThrows
  private void updateProcessInstanceNestedDocLimit(
      final String processDefinitionKey, final int nestedDocLimit) {
    databaseIntegrationTestExtension.updateProcessInstanceNestedDocLimit(
        processDefinitionKey, nestedDocLimit, embeddedOptimizeExtension.getConfigurationService());
  }

  private ProcessInstanceEngineDto deployAndStartSimpleTwoUserTaskProcess() {
    return deployAndStartSimpleTwoUserTaskProcessWithVariables(Collections.emptyMap());
  }

  private ProcessInstanceEngineDto deployAndStartSimpleTwoUserTaskProcessWithVariables(
      final Map<String, Object> variables) {
    return engineIntegrationExtension.deployAndStartProcessWithVariables(
        BpmnModels.getDoubleUserTaskDiagram(PROC_DEF_KEY), variables);
  }
}
