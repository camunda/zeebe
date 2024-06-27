/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.processinstance.duration.groupby.none.distributedby;
//
// import static io.camunda.optimize.dto.optimize.ReportConstants.GROUP_NONE_KEY;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.AVERAGE;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MAX;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MIN;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.PERCENTILE;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.SUM;
// import static
// io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE_BY_PROCESS;
// import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
// import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
// import static java.util.Collections.singletonList;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.db.es.report.util.HyperMapAsserter;
// import io.camunda.optimize.service.util.IdGenerator;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.test.util.DateCreationFreezer;
// import java.time.OffsetDateTime;
// import java.util.Arrays;
// import java.util.List;
// import org.assertj.core.groups.Tuple;
// import org.junit.jupiter.api.Test;
//
// public class ProcessInstanceDurationByNoneByProcessReportEvaluationIT extends AbstractPlatformIT
// {
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSource() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleProcess("aProcess");
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         processInstanceDto.getId(), now.minusSeconds(1), now);
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(
//             processIdentifier, processInstanceDto.getProcessDefinitionKey(), processDisplayName);
//     final ProcessReportDataDto reportData =
//         createDurationGroupedByNoneByProcessReport(List.of(definition));
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.DURATION, AVERAGE)
//         .groupByContains(GROUP_NONE_KEY)
//         .distributedByContains(processIdentifier, 1000., processDisplayName)
//         .doAssert(evaluationResponse.getResult());
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSourcesWithSameName() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleProcess("aProcess");
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         processInstanceDto.getId(), now.minusSeconds(1), now);
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String firstProcessIdentifier = "first";
//     final String secondProcessIdentifier = "second";
//     ReportDataDefinitionDto firstDefinition =
//         new ReportDataDefinitionDto(
//             firstProcessIdentifier,
//             processInstanceDto.getProcessDefinitionKey(),
//             processDisplayName);
//     ReportDataDefinitionDto secondDefinition =
//         new ReportDataDefinitionDto(
//             secondProcessIdentifier,
//             processInstanceDto.getProcessDefinitionKey(),
//             processDisplayName);
//     final ProcessReportDataDto reportData =
//         createDurationGroupedByNoneByProcessReport(List.of(firstDefinition, secondDefinition));
//
//     // when
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
//         .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 new AggregationDto(AVERAGE),
//                 createHyperMapEntries(
//                     new MapResultEntryDto(firstProcessIdentifier, 1000.0, processDisplayName),
//                     new MapResultEntryDto(secondProcessIdentifier, 1000.0,
// processDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSourceWithMultipleMeasures() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto firstInstance = deployAndStartSimpleProcess("aProcess");
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         firstInstance.getId(), now.minusSeconds(10), now);
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.startProcessInstance(firstInstance.getDefinitionId());
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         secondInstance.getId(), now.minusSeconds(2), now);
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(
//             processIdentifier, firstInstance.getProcessDefinitionKey(), processDisplayName);
//     final ProcessReportDataDto reportData =
//         createDurationGroupedByNoneByProcessReport(List.of(definition));
//     reportData
//         .getConfiguration()
//         .setAggregationTypes(
//             new AggregationDto(MAX),
//             new AggregationDto(MIN),
//             new AggregationDto(AVERAGE),
//             new AggregationDto(SUM),
//             new AggregationDto(PERCENTILE, 50.),
//             new AggregationDto(PERCENTILE, 99.));
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(2);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
//     assertThat(result.getMeasures())
//         .hasSize(6)
//         .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 new AggregationDto(MAX),
//                 createHyperMapEntry(processDisplayName, processIdentifier, 10000.0)),
//             Tuple.tuple(
//                 new AggregationDto(MIN),
//                 createHyperMapEntry(processDisplayName, processIdentifier, 2000.0)),
//             Tuple.tuple(
//                 new AggregationDto(AVERAGE),
//                 createHyperMapEntry(processDisplayName, processIdentifier, 6000.0)),
//             Tuple.tuple(
//                 new AggregationDto(SUM),
//                 createHyperMapEntry(processDisplayName, processIdentifier, 12000.0)),
//             // We can't work out percentile aggregation types, so it has a null value
//             Tuple.tuple(
//                 new AggregationDto(PERCENTILE, 50.),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)),
//             Tuple.tuple(
//                 new AggregationDto(PERCENTILE, 99.),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)));
//   }
//
//   @Test
//   public void
//       reportEvaluationWithSingleProcessDefinitionSourceWithMultipleMeasuresAndNoInstances() {
//     // given
//     final ProcessDefinitionEngineDto procDef =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(getSimpleBpmnDiagram());
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String processIdentifier = IdGenerator.getNextId();
//     ReportDataDefinitionDto definition =
//         new ReportDataDefinitionDto(processIdentifier, procDef.getKey(), processDisplayName);
//     final ProcessReportDataDto reportData =
//         createDurationGroupedByNoneByProcessReport(List.of(definition));
//     reportData
//         .getConfiguration()
//         .setAggregationTypes(
//             new AggregationDto(MAX),
//             new AggregationDto(MIN),
//             new AggregationDto(AVERAGE),
//             new AggregationDto(SUM),
//             new AggregationDto(PERCENTILE, 50.),
//             new AggregationDto(PERCENTILE, 99.));
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isZero();
//     assertThat(result.getInstanceCountWithoutFilters()).isZero();
//     assertThat(result.getMeasures())
//         .hasSize(6)
//         .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 new AggregationDto(MAX),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)),
//             Tuple.tuple(
//                 new AggregationDto(MIN),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)),
//             Tuple.tuple(
//                 new AggregationDto(AVERAGE),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)),
//             Tuple.tuple(
//                 new AggregationDto(SUM),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)),
//             Tuple.tuple(
//                 new AggregationDto(PERCENTILE, 50.),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)),
//             Tuple.tuple(
//                 new AggregationDto(PERCENTILE, 99.),
//                 createHyperMapEntry(processDisplayName, processIdentifier, null)));
//   }
//
//   @Test
//   public void
//
// reportEvaluationWithMultipleProcessDefinitionSourceWithMultipleMeasuresAndNoInstancesForOne() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessDefinitionEngineDto firstDef =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             getDoubleUserTaskDiagram("firstProcess"));
//     final ProcessDefinitionEngineDto secondDef =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             getDoubleUserTaskDiagram("firstProcess"));
//     final ProcessInstanceEngineDto instanceForSecondDef =
//         engineIntegrationExtension.startProcessInstance(secondDef.getId());
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         instanceForSecondDef.getId(), now.minusSeconds(2), now);
//     importAllEngineEntitiesFromScratch();
//     final String processDisplayName = "processDisplayName";
//     final String firstIdentifier = "first";
//     final String secondIdentifier = "second";
//     ReportDataDefinitionDto first =
//         new ReportDataDefinitionDto(firstIdentifier, firstDef.getKey(), processDisplayName);
//     first.setVersion("1");
//     ReportDataDefinitionDto second =
//         new ReportDataDefinitionDto(secondIdentifier, secondDef.getKey(), processDisplayName);
//     final ProcessReportDataDto reportData =
//         createDurationGroupedByNoneByProcessReport(List.of(first, second));
//
//     // when
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
//         .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 new AggregationDto(AVERAGE),
//                 createHyperMapEntries(
//                     new MapResultEntryDto(firstIdentifier, null, processDisplayName),
//                     new MapResultEntryDto(secondIdentifier, 2000.0, processDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSourcesAndMultipleMeasures() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto firstDefFirstInstance =
// deployAndStartSimpleProcess("aProcess");
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         firstDefFirstInstance.getId(), now.minusSeconds(10), now);
//     final ProcessInstanceEngineDto firstDefSecondInstance =
//         engineIntegrationExtension.startProcessInstance(firstDefFirstInstance.getDefinitionId());
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         firstDefSecondInstance.getId(), now.minusSeconds(2), now);
//     importAllEngineEntitiesFromScratch();
//     final String firstProcessDisplayName = "processDisplayName1";
//     final String firstProcessIdentifier = "first";
//     ReportDataDefinitionDto firstDefinition =
//         new ReportDataDefinitionDto(
//             firstProcessIdentifier,
//             firstDefFirstInstance.getProcessDefinitionKey(),
//             firstProcessDisplayName);
//
//     final ProcessInstanceEngineDto secondDefInstance =
//         deployAndStartSimpleProcess("anotherProcess");
//     engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
//         secondDefInstance.getId(), now.minusSeconds(5), now);
//     importAllEngineEntitiesFromScratch();
//     final String secondProcessDisplayName = "processDisplayName2";
//     final String secondProcessIdentifier = "second";
//     ReportDataDefinitionDto secondDefinition =
//         new ReportDataDefinitionDto(
//             secondProcessIdentifier,
//             secondDefInstance.getProcessDefinitionKey(),
//             secondProcessDisplayName);
//
//     final ProcessReportDataDto reportData =
//         createDurationGroupedByNoneByProcessReport(List.of(firstDefinition, secondDefinition));
//     reportData
//         .getConfiguration()
//         .setAggregationTypes(new AggregationDto(MAX), new AggregationDto(MIN));
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(3);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(3);
//     assertThat(result.getMeasures())
//         .hasSize(2)
//         .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 new AggregationDto(MAX),
//                 createHyperMapEntries(
//                     new MapResultEntryDto(firstProcessIdentifier, 10000.0,
// firstProcessDisplayName),
//                     new MapResultEntryDto(
//                         secondProcessIdentifier, 5000.0, secondProcessDisplayName))),
//             Tuple.tuple(
//                 new AggregationDto(MIN),
//                 createHyperMapEntries(
//                     new MapResultEntryDto(firstProcessIdentifier, 2000.0,
// firstProcessDisplayName),
//                     new MapResultEntryDto(
//                         secondProcessIdentifier, 5000.0, secondProcessDisplayName))));
//   }
//
//   private List<HyperMapResultEntryDto> createHyperMapEntry(
//       final String processDisplayName, final String processIdentifier, final Double value) {
//     return createHyperMapEntries(
//         new MapResultEntryDto(processIdentifier, value, processDisplayName));
//   }
//
//   private List<HyperMapResultEntryDto> createHyperMapEntries(
//       final MapResultEntryDto... mapResultEntryDtos) {
//     return singletonList(
//         new HyperMapResultEntryDto(
//             GROUP_NONE_KEY, Arrays.asList(mapResultEntryDtos), GROUP_NONE_KEY));
//   }
//
//   private ProcessReportDataDto createDurationGroupedByNoneByProcessReport(
//       final List<ReportDataDefinitionDto> definitionDtos) {
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE_BY_PROCESS)
//             .build();
//     reportData.setDefinitions(definitionDtos);
//     return reportData;
//   }
//
//   private ProcessInstanceEngineDto deployAndStartSimpleProcess(final String processId) {
//     return engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(processId));
//   }
// }
