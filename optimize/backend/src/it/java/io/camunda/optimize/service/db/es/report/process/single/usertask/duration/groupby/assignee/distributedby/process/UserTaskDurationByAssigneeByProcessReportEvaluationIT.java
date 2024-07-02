/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.assignee.distributedby.process;
//
// import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.AVERAGE;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MAX;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static
// io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.identity.ProcessDistributedByIdentity.DISTRIBUTE_BY_IDENTITY_MISSING_KEY;
// import static
// io.camunda.optimize.service.util.ProcessReportDataType.USER_TASK_DUR_GROUP_BY_ASSIGNEE_BY_PROCESS;
// import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
// import static io.camunda.optimize.util.BpmnModels.getFourUserTaskDiagram;
// import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.util.IdGenerator;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import java.util.Collections;
// import java.util.List;
// import org.assertj.core.groups.Tuple;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
//
// public class UserTaskDurationByAssigneeByProcessReportEvaluationIT extends AbstractPlatformIT {
//
//   private static final String SECOND_USER = "secondUser";
//   private static final String SECOND_USER_FIRST_NAME = "the";
//   private static final String SECOND_USER_LAST_NAME = "other";
//   private static final String SECOND_USER_FULL_NAME =
//       SECOND_USER_FIRST_NAME + " " + SECOND_USER_LAST_NAME;
//
//   @BeforeEach
//   public void init() {
//     // create second user
//     engineIntegrationExtension.addUser(SECOND_USER, SECOND_USER_FIRST_NAME,
// SECOND_USER_LAST_NAME);
//     engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
//   }
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSource() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition("aProcess");
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTask1AWithDefaultAndTaskB2WithSecondUser(processInstanceDto);
//     final long setDuration = 20L;
//     changeDuration(processInstanceDto, setDuration);
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(
//             processIdentifier, processDefinition.getKey(), processDisplayName);
//
//     // when
//     final ProcessReportDataDto reportData = createReport(Collections.singletonList(definition));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ProcessReportDataDto resultReportDataDto =
//         evaluationResponse.getReportDefinition().getData();
//
// assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
//     assertThat(resultReportDataDto.getDefinitionVersions())
//         .containsExactly(definition.getVersions().get(0));
//     assertThat(resultReportDataDto.getView()).isNotNull();
//     assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
//     assertThat(resultReportDataDto.getConfiguration().getUserTaskDurationTimes())
//         .containsExactly(UserTaskDurationTime.TOTAL);
//     assertThat(resultReportDataDto.getDistributedBy().getType())
//         .isEqualTo(DistributedByType.PROCESS);
//
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(1);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .extracting(MeasureResponseDto::getData)
//         .containsExactly(
//             List.of(
//                 createHyperMapResult(
//                     DEFAULT_USERNAME,
//                     DEFAULT_FULLNAME,
//                     new MapResultEntryDto(processIdentifier, 20., processDisplayName)),
//                 createHyperMapResult(
//                     SECOND_USER,
//                     SECOND_USER_FULL_NAME,
//                     new MapResultEntryDto(processIdentifier, 20., processDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSourceWithUnassignedUser() {
//     // given
//     final ProcessDefinitionEngineDto process = deployFourUserTasksDefinition("process");
//     final ProcessInstanceEngineDto instance =
//         engineIntegrationExtension.startProcessInstance(process.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, instance.getId());
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(processIdentifier, process.getKey(), processDisplayName);
//
//     // when
//     final ProcessReportDataDto reportData = createReport(Collections.singletonList(definition));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(1);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .flatMap(MeasureResponseDto::getData)
//         .extracting(HyperMapResultEntryDto::getKey, HyperMapResultEntryDto::getLabel)
//         .containsExactly(
//             // We cannot assert on the actual values as unassigned tasks are still running
//             Tuple.tuple(DEFAULT_USERNAME, DEFAULT_FULLNAME),
//             Tuple.tuple(DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel()));
//   }
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSourceWithAllInstancesRemovedByFilter()
// {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition("aProcess");
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTask1AWithDefaultAndTaskB2WithSecondUser(processInstanceDto);
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(
//             processIdentifier, processDefinition.getKey(), processDisplayName);
//
//     // when
//     final ProcessReportDataDto reportData = createReport(Collections.singletonList(definition));
//
// reportData.setFilter(ProcessFilterBuilder.filter().canceledInstancesOnly().add().buildList());
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isZero();
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .extracting(MeasureResponseDto::getData)
//         .containsExactly(Collections.emptyList());
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSources() {
//     // given
//     final ProcessDefinitionEngineDto firstProcess = deployFourUserTasksDefinition("first");
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.startProcessInstance(firstProcess.getId());
//     finishUserTask1AWithDefaultAndTaskB2WithSecondUser(firstInstance);
//     changeDuration(firstInstance, 50.);
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("second"));
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
//     changeDuration(secondInstance, 10.);
//     importAllEngineEntitiesFromScratch();
//     final String firstDisplayName = "firstName";
//     final String secondDisplayName = "secondName";
//     final String firstIdentifier = "first";
//     final String secondIdentifier = "second";
//     ReportDataDefinitionDto firstDefinition =
//         new ReportDataDefinitionDto(firstIdentifier, firstProcess.getKey(), firstDisplayName);
//     ReportDataDefinitionDto secondDefinition =
//         new ReportDataDefinitionDto(
//             secondIdentifier, secondInstance.getProcessDefinitionKey(), secondDisplayName);
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(List.of(firstDefinition, secondDefinition));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(2);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .extracting(MeasureResponseDto::getData)
//         .containsExactly(
//             List.of(
//                 createHyperMapResult(
//                     DEFAULT_USERNAME,
//                     DEFAULT_FULLNAME,
//                     new MapResultEntryDto(firstIdentifier, 50., firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, 10., secondDisplayName)),
//                 createHyperMapResult(
//                     SECOND_USER,
//                     SECOND_USER_FULL_NAME,
//                     new MapResultEntryDto(firstIdentifier, 50., firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstances() {
//     // given
//     final ProcessDefinitionEngineDto v1Process = deployFourUserTasksDefinition("definition");
//     final ProcessInstanceEngineDto v1Instance =
//         engineIntegrationExtension.startProcessInstance(v1Process.getId());
//     finishUserTask1AWithDefaultAndTaskB2WithSecondUser(v1Instance);
//     changeDuration(v1Instance, 50.);
//     final ProcessInstanceEngineDto v2Instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, v2Instance.getId());
//     changeDuration(v2Instance, 20.);
//     importAllEngineEntitiesFromScratch();
//     final String v1displayName = "v1";
//     final String allVersionsDisplayName = "all";
//     final String v1Identifier = "v1Identifier";
//     final String allVersionsIdentifier = "allIdentifier";
//     ReportDataDefinitionDto v1definition =
//         new ReportDataDefinitionDto(v1Identifier, v1Process.getKey(), v1displayName);
//     v1definition.setVersion("1");
//     ReportDataDefinitionDto allVersionsDefinition =
//         new ReportDataDefinitionDto(
//             allVersionsIdentifier, v2Instance.getProcessDefinitionKey(), allVersionsDisplayName);
//     allVersionsDefinition.setVersion(ALL_VERSIONS);
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(List.of(v1definition, allVersionsDefinition));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(2);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .extracting(MeasureResponseDto::getData)
//         .containsExactly(
//             List.of(
//                 createHyperMapResult(
//                     DEFAULT_USERNAME,
//                     DEFAULT_FULLNAME,
//                     new MapResultEntryDto(allVersionsIdentifier, 40., allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, 50., v1displayName)),
//                 createHyperMapResult(
//                     SECOND_USER,
//                     SECOND_USER_FULL_NAME,
//                     new MapResultEntryDto(allVersionsIdentifier, 50., allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, 50., v1displayName))));
//   }
//
//   @Test
//   public void
//
// reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstancesAcrossAggregations() {
//     // given
//     final ProcessDefinitionEngineDto v1Process = deployFourUserTasksDefinition("definition");
//     final ProcessInstanceEngineDto v1Instance =
//         engineIntegrationExtension.startProcessInstance(v1Process.getId());
//     finishUserTask1AWithDefaultAndTaskB2WithSecondUser(v1Instance);
//     changeDuration(v1Instance, 50.);
//     final ProcessInstanceEngineDto v2Instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, v2Instance.getId());
//     changeDuration(v2Instance, 20.);
//     importAllEngineEntitiesFromScratch();
//     final String v1displayName = "v1";
//     final String allVersionsDisplayName = "all";
//     final String v1Identifier = "v1Identifier";
//     final String allVersionsIdentifier = "allIdentifier";
//     ReportDataDefinitionDto v1definition =
//         new ReportDataDefinitionDto(v1Identifier, v1Process.getKey(), v1displayName);
//     v1definition.setVersion("1");
//     ReportDataDefinitionDto allVersionsDefinition =
//         new ReportDataDefinitionDto(
//             allVersionsIdentifier, v2Instance.getProcessDefinitionKey(), allVersionsDisplayName);
//     allVersionsDefinition.setVersion(ALL_VERSIONS);
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(List.of(v1definition, allVersionsDefinition));
//     reportData
//         .getConfiguration()
//         .setAggregationTypes(new AggregationDto(AVERAGE), new AggregationDto(MAX));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(2);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
//     assertThat(result.getMeasures())
//         .hasSize(2)
//         .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 new AggregationDto(AVERAGE),
//                 List.of(
//                     createHyperMapResult(
//                         DEFAULT_USERNAME,
//                         DEFAULT_FULLNAME,
//                         new MapResultEntryDto(allVersionsIdentifier, 40.,
// allVersionsDisplayName),
//                         new MapResultEntryDto(v1Identifier, 50., v1displayName)),
//                     createHyperMapResult(
//                         SECOND_USER,
//                         SECOND_USER_FULL_NAME,
//                         new MapResultEntryDto(allVersionsIdentifier, 50.,
// allVersionsDisplayName),
//                         new MapResultEntryDto(v1Identifier, 50., v1displayName)))),
//             Tuple.tuple(
//                 new AggregationDto(MAX),
//                 List.of(
//                     createHyperMapResult(
//                         DEFAULT_USERNAME,
//                         DEFAULT_FULLNAME,
//                         new MapResultEntryDto(allVersionsIdentifier, 50.,
// allVersionsDisplayName),
//                         new MapResultEntryDto(v1Identifier, 50., v1displayName)),
//                     createHyperMapResult(
//                         SECOND_USER,
//                         SECOND_USER_FULL_NAME,
//                         new MapResultEntryDto(allVersionsIdentifier, 50.,
// allVersionsDisplayName),
//                         new MapResultEntryDto(v1Identifier, 50., v1displayName)))));
//   }
//
//   private void changeDuration(
//       final ProcessInstanceEngineDto processInstanceDto, final double durationInMs) {
//     engineDatabaseExtension.changeAllFlowNodeTotalDurations(
//         processInstanceDto.getId(), durationInMs);
//   }
//
//   private ProcessDefinitionEngineDto deployFourUserTasksDefinition(final String aProcessName) {
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//         getFourUserTaskDiagram(aProcessName));
//   }
//
//   private void finishUserTask1AWithDefaultAndTaskB2WithSecondUser(
//       final ProcessInstanceEngineDto processInstanceDto) {
//     // finish user task 1 and A with default user
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
//     // finish user task 2 and B with second user
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USER, processInstanceDto.getId());
//   }
//
//   private HyperMapResultEntryDto createHyperMapResult(
//       final String key, final String label, final MapResultEntryDto... results) {
//     return new HyperMapResultEntryDto(key, List.of(results), label);
//   }
//
//   private ProcessReportDataDto createReport(final List<ReportDataDefinitionDto> definitionDtos) {
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setUserTaskDurationTime(UserTaskDurationTime.TOTAL)
//             .setReportDataType(USER_TASK_DUR_GROUP_BY_ASSIGNEE_BY_PROCESS)
//             .build();
//     reportData.setDefinitions(definitionDtos);
//     return reportData;
//   }
//
//   private String getLocalizedUnassignedLabel() {
//     return embeddedOptimizeExtension
//         .getLocalizationService()
//         .getDefaultLocaleMessageForMissingAssigneeLabel();
//   }
// }
