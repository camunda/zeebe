/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter.process;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.util.List;
import java.util.stream.Stream;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class InstancesContainingUserTasksFilterIT extends AbstractFilterIT {

  private static Stream<ProcessReportDataType> userTaskReports() {
    return ProcessReportDataType.allViewUserTaskReports().stream();
  }

  @ParameterizedTest
  @MethodSource("userTaskReports")
  public void filterInstancesContainingUserTasksForUserTaskReport(
      final ProcessReportDataType userTaskReportType) {
    // given one instance that has a userTask and one instance that has no userTasks
    final BpmnModelInstance userTaskProcess = createOptionalUserTaskProcess();
    ProcessDefinitionEngineDto userTaskProcessDef =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(userTaskProcess);
    engineIntegrationExtension.startProcessInstance(
        userTaskProcessDef.getId(), ImmutableMap.of("continueToUserTask", true));
    engineIntegrationExtension.startProcessInstance(
        userTaskProcessDef.getId(), ImmutableMap.of("continueToUserTask", false));
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(userTaskProcessDef.getKey())
            .setProcessDefinitionVersion(userTaskProcessDef.getVersionAsString())
            .setGroupByDateInterval(AggregateByDateUnit.DAY)
            .setDistributeByDateInterval(AggregateByDateUnit.DAY)
            .setReportDataType(userTaskReportType)
            .build();
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        reportClient.evaluateRawReport(reportData).getResult();

    // then the userTask report automatically includes a InstancesContainingUserTasks filter and
    // only one instance is
    // in the filtered result
    assertThat(result.getInstanceCount()).isEqualTo(1);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
  }

  private BpmnModelInstance createOptionalUserTaskProcess() {
    // @formatter:off
    return Bpmn.createExecutableProcess("aUserTaskProcess")
        .camundaVersionTag("1")
        .name("aProcessDefKey")
        .startEvent("start")
        .exclusiveGateway("exclusiveGateWay")
        .condition("gotToUserTask", "${continueToUserTask}")
        .userTask("userTask1")
        .endEvent("end")
        .moveToLastGateway()
        .condition("goToServiceTask", "${!continueToUserTask}")
        .serviceTask("serviceTask")
        .camundaExpression("${true}")
        .connectTo("end")
        .done();
    // @formatter:on
  }
}
