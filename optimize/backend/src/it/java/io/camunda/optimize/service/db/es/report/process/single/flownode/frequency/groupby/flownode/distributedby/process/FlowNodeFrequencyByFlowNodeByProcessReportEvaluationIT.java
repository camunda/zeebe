/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.flownode.frequency.groupby.flownode.distributedby.process;
//
// import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
// import static
// io.camunda.optimize.service.util.ProcessReportDataType.FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE_BY_PROCESS;
// import static io.camunda.optimize.util.BpmnModels.END_EVENT;
// import static io.camunda.optimize.util.BpmnModels.SERVICE_TASK;
// import static io.camunda.optimize.util.BpmnModels.START_EVENT;
// import static io.camunda.optimize.util.BpmnModels.USER_TASK_1;
// import static io.camunda.optimize.util.BpmnModels.getSingleServiceTaskProcess;
// import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
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
// import org.junit.jupiter.api.Test;
//
// public class FlowNodeFrequencyByFlowNodeByProcessReportEvaluationIT extends AbstractPlatformIT {
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSource() {
//     // given
//     final ProcessInstanceEngineDto instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("first"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(
//             processIdentifier, instance.getProcessDefinitionKey(), processDisplayName);
//
//     // when
//     final ProcessReportDataDto reportData = createReport(Collections.singletonList(definition));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ProcessReportDataDto resultReportDataDto =
//         evaluationResponse.getReportDefinition().getData();
//     assertThat(resultReportDataDto.getProcessDefinitionKey())
//         .isEqualTo(instance.getProcessDefinitionKey());
//     assertThat(resultReportDataDto.getDefinitionVersions())
//         .containsExactly(definition.getVersions().get(0));
//     assertThat(resultReportDataDto.getView()).isNotNull();
//     assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.FLOW_NODE);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.FREQUENCY);
//     assertThat(resultReportDataDto.getGroupBy()).isNotNull();
//
// assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.FLOW_NODES);
//     assertThat(resultReportDataDto.getGroupBy().getValue()).isNull();
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
//                     END_EVENT, new MapResultEntryDto(processIdentifier, 1.0,
// processDisplayName)),
//                 createHyperMapResult(
//                     START_EVENT, new MapResultEntryDto(processIdentifier, 1.0,
// processDisplayName)),
//                 createHyperMapResult(
//                     USER_TASK_1,
//                     new MapResultEntryDto(processIdentifier, 1.0, processDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSourceWithAllInstancesRemovedByFilter()
// {
//     // given
//     final ProcessInstanceEngineDto instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("first"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(
//             processIdentifier, instance.getProcessDefinitionKey(), processDisplayName);
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
//         .containsExactly(
//             List.of(
//                 createHyperMapResult(
//                     END_EVENT, new MapResultEntryDto(processIdentifier, null,
// processDisplayName)),
//                 createHyperMapResult(
//                     START_EVENT,
//                     new MapResultEntryDto(processIdentifier, null, processDisplayName)),
//                 createHyperMapResult(
//                     USER_TASK_1,
//                     new MapResultEntryDto(processIdentifier, null, processDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSources() {
//     // given
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("first"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleServiceTaskProcess("second"));
//     importAllEngineEntitiesFromScratch();
//     final String firstDisplayName = "firstName";
//     final String secondDisplayName = "secondName";
//     final String firstIdentifier = "first";
//     final String secondIdentifier = "second";
//     ReportDataDefinitionDto firstDefinition =
//         new ReportDataDefinitionDto(
//             firstIdentifier, firstInstance.getProcessDefinitionKey(), firstDisplayName);
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
//                     END_EVENT,
//                     new MapResultEntryDto(firstIdentifier, 1.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, 1.0, secondDisplayName)),
//                 createHyperMapResult(
//                     SERVICE_TASK,
//                     new MapResultEntryDto(firstIdentifier, null, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, 1.0, secondDisplayName)),
//                 createHyperMapResult(
//                     START_EVENT,
//                     new MapResultEntryDto(firstIdentifier, 1.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, 1.0, secondDisplayName)),
//                 createHyperMapResult(
//                     USER_TASK_1,
//                     new MapResultEntryDto(firstIdentifier, 1.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSources_oneWithoutInstanceStarted() {
//     // given
//     final ProcessInstanceEngineDto firstDefinitionInstance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("first"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     final ProcessDefinitionEngineDto secondDefinitionNoInstance =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             getSingleServiceTaskProcess("second"));
//     // We deploy this to create an index, but it is not part of the report definition so should
// be
//     // excluded
//     engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("third"));
//     importAllEngineEntitiesFromScratch();
//     final String firstDisplayName = "firstName";
//     final String secondDisplayName = "secondName";
//     final String firstIdentifier = "first";
//     final String secondIdentifier = "second";
//     ReportDataDefinitionDto firstDefinition =
//         new ReportDataDefinitionDto(
//             firstIdentifier, firstDefinitionInstance.getProcessDefinitionKey(),
// firstDisplayName);
//     ReportDataDefinitionDto secondDefinition =
//         new ReportDataDefinitionDto(
//             secondIdentifier, secondDefinitionNoInstance.getKey(), secondDisplayName);
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
//     assertThat(result.getInstanceCount()).isEqualTo(1);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .extracting(MeasureResponseDto::getData)
//         .containsExactly(
//             List.of(
//                 createHyperMapResult(
//                     END_EVENT,
//                     new MapResultEntryDto(firstIdentifier, 1.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName)),
//                 createHyperMapResult(
//                     SERVICE_TASK,
//                     new MapResultEntryDto(firstIdentifier, null, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName)),
//                 createHyperMapResult(
//                     START_EVENT,
//                     new MapResultEntryDto(firstIdentifier, 1.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName)),
//                 createHyperMapResult(
//                     USER_TASK_1,
//                     new MapResultEntryDto(firstIdentifier, 1.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstances() {
//     // given
//     final ProcessInstanceEngineDto v1Instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     final ProcessInstanceEngineDto v2Instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     importAllEngineEntitiesFromScratch();
//     final String v1displayName = "v1";
//     final String allVersionsDisplayName = "all";
//     final String v1Identifier = "v1Identifier";
//     final String allVersionsIdentifier = "allIdentifier";
//     ReportDataDefinitionDto v1definition =
//         new ReportDataDefinitionDto(
//             v1Identifier, v1Instance.getProcessDefinitionKey(), v1displayName);
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
//                     END_EVENT,
//                     new MapResultEntryDto(allVersionsIdentifier, 2.0, allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, 1.0, v1displayName)),
//                 createHyperMapResult(
//                     START_EVENT,
//                     new MapResultEntryDto(allVersionsIdentifier, 2.0, allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, 1.0, v1displayName)),
//                 createHyperMapResult(
//                     USER_TASK_1,
//                     new MapResultEntryDto(allVersionsIdentifier, 2.0, allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, 1.0, v1displayName))));
//   }
//
//   private HyperMapResultEntryDto createHyperMapResult(
//       final String flowNodeId, final MapResultEntryDto... results) {
//     return new HyperMapResultEntryDto(flowNodeId, List.of(results), flowNodeId);
//   }
//
//   private ProcessReportDataDto createReport(final List<ReportDataDefinitionDto> definitionDtos) {
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE_BY_PROCESS)
//             .build();
//     reportData.setDefinitions(definitionDtos);
//     return reportData;
//   }
// }
