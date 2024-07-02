/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.export;

import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.rest.RestTestUtil.getResponseContentAsString;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.service.util.configuration.users.AuthorizedUserType;
import io.camunda.optimize.util.FileReaderUtil;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CombinedProcessCsvExportServiceIT extends AbstractPlatformIT {

  private static final String START = "aStart";
  private static final String END = "anEnd";
  private static final String VARIABLE_NAME = "var";

  @ParameterizedTest
  @ValueSource(strings = {"SUPERUSER", "NONE"})
  public void csvExportForbiddenWhenDisabledForNonSuperuserInConfig(
      final String authorizationType) {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getCsvConfiguration()
        .setAuthorizedUserType(AuthorizedUserType.valueOf(authorizationType));
    embeddedOptimizeExtension.reloadConfiguration();
    final String reportId = reportClient.createEmptyCombinedReport(null);

    // when
    Response response = exportClient.exportReportAsCsv(reportId, "my_file.csv", "Etc/GMT-1");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void csvExportForbiddenWhenDisabledForSuperuserInConfig() {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAuthConfiguration()
        .setSuperUserIds(Collections.singletonList(DEFAULT_USERNAME));
    embeddedOptimizeExtension
        .getConfigurationService()
        .getCsvConfiguration()
        .setAuthorizedUserType(AuthorizedUserType.NONE);
    embeddedOptimizeExtension.reloadConfiguration();
    final String reportId = reportClient.createEmptyCombinedReport(null);

    // when
    Response response = exportClient.exportReportAsCsv(reportId, "my_file.csv", "Etc/GMT-1");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"SUPERUSER", "ALL"})
  public void csvExportWorksForSuperuserWhenAuthorizedInConfig(final String authorizationType) {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAuthConfiguration()
        .setSuperUserIds(Collections.singletonList(DEFAULT_USERNAME));
    embeddedOptimizeExtension
        .getConfigurationService()
        .getCsvConfiguration()
        .setAuthorizedUserType(AuthorizedUserType.valueOf(authorizationType));
    embeddedOptimizeExtension.reloadConfiguration();

    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram("aProcess", START, END));
    String singleReportId1 = createNewSingleNumberReport(processInstance1);
    String singleReportId2 = createNewSingleNumberReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response =
        exportClient.exportReportAsCsv(combinedReportId, "my_file.csv", "Etc/GMT-1");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void combinedMapReport_missingFlowNodesAreAutomaticallyAdded() {
    // given
    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram("aProcess", START, END));
    String singleReportId1 = createNewSingleMapReport(processInstance1);
    String singleReportId2 = createNewSingleMapReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_flow_node_frequency_group_by_flow_node.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedMapReport_missingVariablesAreAutomaticallyAdded() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put(VARIABLE_NAME, "val1");
    ProcessInstanceEngineDto processInstance1 =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSimpleBpmnDiagram(), variables);
    variables.put(VARIABLE_NAME, "val1");
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSimpleBpmnDiagram(), variables);
    variables.put(VARIABLE_NAME, "val2");
    ProcessInstanceEngineDto processInstance3 =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSimpleBpmnDiagram(), variables);
    String singleReportId1 = createNewGroupedByVariableReport(processInstance1);
    String singleReportId2 = createNewGroupedByVariableReport(processInstance2);
    String singleReportId3 = createNewGroupedByVariableReport(processInstance3);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2, singleReportId3);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_flow_node_frequency_group_by_variable.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedMapReport_useLabelsForResultIfAvailable() {
    // given
    // @formatter:off
    final BpmnModelInstance definition =
        Bpmn.createExecutableProcess("aProcess")
            .name("aProcess")
            .startEvent("firstFlowNodeKey")
            .name("firstFlowNodeName")
            .serviceTask("myServiceTaskWithoutName")
            .camundaExpression("${true}")
            .endEvent("secondFlowNodeKey")
            .name("secondFlowNodeName")
            .done();
    // @formatter:on
    ProcessInstanceEngineDto processInstance =
        engineIntegrationExtension.deployAndStartProcess(definition);
    String singleReportId1 = createNewSingleMapReport(processInstance);
    String singleReportId2 = createNewSingleMapReport(processInstance);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_report_useLabelsInResult.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedDurationMapReportHasExpectedValue() {
    // given
    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(processInstance1.getId(), 0);
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram("aProcess", START, END));
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(processInstance2.getId(), 0);
    String singleReportId1 = createNewSingleDurationMapReport(processInstance1);
    String singleReportId2 = createNewSingleDurationMapReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_flow_node_duration_group_by_flow_node.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void theOrderOfTheReportsDoesMatter() {
    // given
    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram("aProcess", START, END));
    String singleReportId1 = createNewSingleMapReport(processInstance1);
    String singleReportId2 = createNewSingleMapReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId2, singleReportId1);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_flow_node_frequency_group_by_flow_node_different_order.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedNumberReportHasExpectedValue() {
    // given
    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram("aProcess", START, END));
    String singleReportId1 = createNewSingleNumberReport(processInstance1);
    String singleReportId2 = createNewSingleNumberReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_pi_frequency_group_by_none.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedDurationNumberReportHasExpectedValue() {
    // given
    final OffsetDateTime startDate = OffsetDateTime.now();
    final OffsetDateTime endDate = startDate.plus(1, ChronoUnit.MILLIS);
    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstance1.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(processInstance1.getId(), endDate);
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram());
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstance2.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(processInstance2.getId(), endDate);
    String singleReportId1 = createNewSingleDurationNumberReport(processInstance1);
    String singleReportId2 = createNewSingleDurationNumberReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_pi_duration_group_by_none.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedReportWithUnevaluatableReportProducesEmptyResult() {
    // given
    String singleReportId1 = reportClient.createEmptySingleProcessReportInCollection(null);
    String combinedReportId = reportClient.createNewCombinedReport(singleReportId1);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_empty_report.csv");

    assertThat(actualContent).isEqualTo(stringExpected);
  }

  @Test
  public void combinedReportWithoutReportsProducesEmptyResult() {
    // given
    String combinedReportId = reportClient.createEmptyCombinedReport(null);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    String actualContent = getResponseContentAsString(response);
    assertThat(actualContent.trim()).isEmpty();
  }

  @Test
  public void combinedReportCsvExportWorksWithSemiColonDelimiter() {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getCsvConfiguration()
        .setExportCsvDelimiter(';');
    final OffsetDateTime startDate = OffsetDateTime.now();
    final OffsetDateTime endDate = startDate.plus(1, ChronoUnit.MILLIS);
    ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcessWith5FlowNodes();
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstance1.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(processInstance1.getId(), endDate);
    ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram());
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstance2.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(processInstance2.getId(), endDate);
    String singleReportId1 = createNewSingleDurationNumberReport(processInstance1);
    String singleReportId2 = createNewSingleDurationNumberReport(processInstance2);
    String combinedReportId =
        reportClient.createNewCombinedReport(singleReportId1, singleReportId2);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = exportClient.exportReportAsCsv(combinedReportId, "my_file.csv");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    String actualContent = getResponseContentAsString(response);
    String stringExpected =
        FileReaderUtil.readFileWithWindowsLineSeparator(
            "/csv/process/combined/combined_semicolon_delimiter.csv");
    assertThat(actualContent).isEqualTo(stringExpected);
  }

  private String createNewSingleMapReport(ProcessInstanceEngineDto engineDto) {
    ProcessReportDataDto countFlowNodeFrequencyGroupByFlowNode =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(engineDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(engineDto.getProcessDefinitionVersion())
            .setReportDataType(ProcessReportDataType.FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE)
            .build();
    return createNewSingleMapReport(countFlowNodeFrequencyGroupByFlowNode);
  }

  private String createNewGroupedByVariableReport(ProcessInstanceEngineDto engineDto) {
    ProcessReportDataDto processInstanceGroupedByVar =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(engineDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(engineDto.getProcessDefinitionVersion())
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_VARIABLE)
            .setVariableType(VariableType.STRING)
            .setVariableName(VARIABLE_NAME)
            .build();
    return createNewSingleMapReport(processInstanceGroupedByVar);
  }

  private String createNewSingleDurationMapReport(ProcessInstanceEngineDto engineDto) {
    ProcessReportDataDto processInstanceDurationGroupByNone =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(engineDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(engineDto.getProcessDefinitionVersion())
            .setReportDataType(ProcessReportDataType.FLOW_NODE_DUR_GROUP_BY_FLOW_NODE)
            .build();
    return createNewSingleMapReport(processInstanceDurationGroupByNone);
  }

  private String createNewSingleMapReport(ProcessReportDataDto data) {
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionDto.setName("FooName");
    singleProcessReportDefinitionDto.setData(data);
    return reportClient.createSingleProcessReport(singleProcessReportDefinitionDto);
  }

  private String createNewSingleNumberReport(ProcessInstanceEngineDto engineDto) {
    ProcessReportDataDto countFlowNodeFrequencyGroupByFlowNode =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(engineDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(engineDto.getProcessDefinitionVersion())
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .build();
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionDto.setData(countFlowNodeFrequencyGroupByFlowNode);
    return reportClient.createSingleProcessReport(singleProcessReportDefinitionDto);
  }

  private String createNewSingleDurationNumberReport(ProcessInstanceEngineDto engineDto) {
    ProcessReportDataDto processInstanceDurationGroupByNone =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(engineDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(engineDto.getProcessDefinitionVersion())
            .setReportDataType(ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE)
            .build();
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionDto.setData(processInstanceDurationGroupByNone);
    return reportClient.createSingleProcessReport(singleProcessReportDefinitionDto);
  }

  private ProcessInstanceEngineDto deployAndStartSimpleProcessWith5FlowNodes() {
    // @formatter:off
    BpmnModelInstance processModel =
        Bpmn.createExecutableProcess("aProcess")
            .name("aProcessName")
            .startEvent(START)
            .serviceTask("ServiceTask1")
            .camundaExpression("${true}")
            .serviceTask("ServiceTask2")
            .camundaExpression("${true}")
            .serviceTask("ServiceTask3")
            .camundaExpression("${true}")
            .endEvent(END)
            .done();
    // @formatter:on
    return engineIntegrationExtension.deployAndStartProcess(processModel);
  }
}
