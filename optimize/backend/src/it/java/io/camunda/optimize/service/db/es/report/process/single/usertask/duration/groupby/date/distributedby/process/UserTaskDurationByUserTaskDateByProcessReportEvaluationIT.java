/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.date.distributedby.process;
//
// import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
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
// import static io.camunda.optimize.test.util.DateModificationHelper.truncateToStartOfUnit;
// import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
// import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
// import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.group.value.DateGroupByValueDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.util.IdGenerator;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.test.util.DateCreationFreezer;
// import java.time.OffsetDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.Collections;
// import java.util.List;
// import org.assertj.core.groups.Tuple;
// import org.junit.jupiter.api.Test;
//
// public abstract class UserTaskDurationByUserTaskDateByProcessReportEvaluationIT
//     extends AbstractPlatformIT {
//
//   private static final String V_1_IDENTIFIER = "v1Identifier";
//   private static final String ALL_VERSIONS_IDENTIFIER = "allVersionsIdentifier";
//   private static final String ALL_DISPLAY_NAME = "all";
//   private static final String V_1_DISPLAY_NAME = "v1";
//
//   protected abstract ProcessReportDataType getReportDataType();
//
//   protected abstract ProcessGroupByType getGroupByType();
//
//   @Test
//   public void reportEvaluationWithSingleProcessDefinitionSource() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDurationAndDatesForInstance(instance, 1000.0, now);
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
//     assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
//     assertThat(resultReportDataDto.getGroupBy()).isNotNull();
//     assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(getGroupByType());
//     assertThat(resultReportDataDto.getGroupBy().getValue())
//         .extracting(DateGroupByValueDto.class::cast)
//         .extracting(DateGroupByValueDto::getUnit)
//         .isEqualTo(AggregateByDateUnit.DAY);
//     assertThat(resultReportDataDto.getDistributedBy().getType())
//         .isEqualTo(DistributedByType.PROCESS);
//
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(1);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
//     assertThat(result.getMeasures())
//         .hasSize(1)
//         .extracting(
//             MeasureResponseDto::getProperty,
//             MeasureResponseDto::getAggregationType,
//             MeasureResponseDto::getData)
//         .containsExactly(
//             Tuple.tuple(
//                 ViewProperty.DURATION,
//                 new AggregationDto(AggregationType.AVERAGE),
//                 List.of(
//                     createHyperMapResult(
//                         localDateTimeToString(now),
//                         new MapResultEntryDto(processIdentifier, 1000.0, processDisplayName)))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSources() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("first"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDurationAndDatesForInstance(firstInstance, 1000.0, now);
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("second"));
//     changeUserTaskDurationAndDatesForInstance(secondInstance, 5000.0, now.minusDays(1));
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
//                     localDateTimeToString(now.minusDays(1)),
//                     new MapResultEntryDto(firstIdentifier, null, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, 5000.0, secondDisplayName)),
//                 createHyperMapResult(
//                     localDateTimeToString(now),
//                     new MapResultEntryDto(firstIdentifier, 1000.0, firstDisplayName),
//                     new MapResultEntryDto(secondIdentifier, null, secondDisplayName))));
//   }
//
//   @Test
//   public void reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstances() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto v1Instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDurationAndDatesForInstance(v1Instance, 1000.0, now);
//     final ProcessInstanceEngineDto v2Instance =
//         engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDurationAndDatesForInstance(v2Instance, 5000.0, now.minusDays(1));
//     engineDatabaseExtension.changeAllFlowNodeTotalDurations(v2Instance.getId(), 5000);
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
//                     localDateTimeToString(now.minusDays(1)),
//                     new MapResultEntryDto(allVersionsIdentifier, 5000.0, allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, null, v1displayName)),
//                 createHyperMapResult(
//                     localDateTimeToString(now),
//                     new MapResultEntryDto(allVersionsIdentifier, 1000.0, allVersionsDisplayName),
//                     new MapResultEntryDto(v1Identifier, 1000.0, v1displayName))));
//   }
//
//   @Test
//   public void
//
// reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstancesAcrossAggregationTypes() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessInstanceEngineDto v1Instance =
//         engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDurationAndDatesForInstance(v1Instance, 1000.0, now);
//     final ProcessInstanceEngineDto v2Instance =
//         engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("definition"));
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDurationAndDatesForInstance(v2Instance, 5000.0, now.minusDays(1));
//     importAllEngineEntitiesFromScratch();
//     ReportDataDefinitionDto v1definition =
//         new ReportDataDefinitionDto(
//             V_1_IDENTIFIER, v1Instance.getProcessDefinitionKey(), V_1_DISPLAY_NAME);
//     v1definition.setVersion("1");
//     ReportDataDefinitionDto allVersionsDefinition =
//         new ReportDataDefinitionDto(
//             ALL_VERSIONS_IDENTIFIER, v2Instance.getProcessDefinitionKey(), ALL_DISPLAY_NAME);
//     allVersionsDefinition.setVersion(ALL_VERSIONS);
//     final ProcessReportDataDto reportData =
//         createReport(List.of(v1definition, allVersionsDefinition));
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
//                 List.of(
//                     versionResults(localDateTimeToString(now.minusDays(1)), 5000.0, null),
//                     versionResults(localDateTimeToString(now), 1000.0, 1000.0))),
//             Tuple.tuple(
//                 new AggregationDto(MIN),
//                 List.of(
//                     versionResults(localDateTimeToString(now.minusDays(1)), 5000.0, null),
//                     versionResults(localDateTimeToString(now), 1000.0, 1000.0))),
//             Tuple.tuple(
//                 new AggregationDto(AVERAGE),
//                 List.of(
//                     versionResults(localDateTimeToString(now.minusDays(1)), 5000.0, null),
//                     versionResults(localDateTimeToString(now), 1000.0, 1000.0))),
//             Tuple.tuple(
//                 new AggregationDto(SUM),
//                 List.of(
//                     versionResults(localDateTimeToString(now.minusDays(1)), 10000.0, null),
//                     versionResults(localDateTimeToString(now), 1000.0, 1000.0))),
//             // We cannot support percentile aggregation types with this distribution as the
//             // information is
//             // lost on merging
//             Tuple.tuple(
//                 new AggregationDto(PERCENTILE, 50.),
//                 List.of(
//                     versionResults(localDateTimeToString(now.minusDays(1)), null, null),
//                     versionResults(localDateTimeToString(now), null, null))),
//             Tuple.tuple(
//                 new AggregationDto(PERCENTILE, 99.),
//                 List.of(
//                     versionResults(localDateTimeToString(now.minusDays(1)), null, null),
//                     versionResults(localDateTimeToString(now), null, null))));
//   }
//
//   private HyperMapResultEntryDto versionResults(
//       final String key, final Double allVersionsResult, final Double v1Result) {
//     return new HyperMapResultEntryDto(
//         key,
//         List.of(
//             new MapResultEntryDto(ALL_VERSIONS_IDENTIFIER, allVersionsResult, ALL_DISPLAY_NAME),
//             new MapResultEntryDto(V_1_IDENTIFIER, v1Result, V_1_DISPLAY_NAME)));
//   }
//
//   private void changeUserTaskDurationAndDatesForInstance(
//       final ProcessInstanceEngineDto processInstanceDto,
//       final Double flowNodeDurations,
//       final OffsetDateTime instanceStartEnd) {
//     engineDatabaseExtension.changeAllFlowNodeStartDates(
//         processInstanceDto.getId(), instanceStartEnd);
//     engineDatabaseExtension.changeAllFlowNodeEndDates(processInstanceDto.getId(),
// instanceStartEnd);
//     engineDatabaseExtension.changeAllFlowNodeTotalDurations(
//         processInstanceDto.getId(), flowNodeDurations.longValue());
//   }
//
//   private String localDateTimeToString(OffsetDateTime time) {
//     return embeddedOptimizeExtension
//         .getDateTimeFormatter()
//         .format(truncateToStartOfUnit(time, ChronoUnit.DAYS));
//   }
//
//   private HyperMapResultEntryDto createHyperMapResult(
//       final String dateAsString, final MapResultEntryDto... results) {
//     return new HyperMapResultEntryDto(dateAsString, List.of(results), dateAsString);
//   }
//
//   private ProcessReportDataDto createReport(final List<ReportDataDefinitionDto> definitionDtos) {
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(getReportDataType())
//             .setGroupByDateInterval(AggregateByDateUnit.DAY)
//             .build();
//     reportData.setDefinitions(definitionDtos);
//     return reportData;
//   }
// }
