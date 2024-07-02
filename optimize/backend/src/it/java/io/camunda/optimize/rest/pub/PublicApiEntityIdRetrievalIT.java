/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// // TODO recreate C8 IT equivalent of this with #13337
// // package io.camunda.optimize.rest.pub;
// //
// // import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// // import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
// // import static io.camunda.optimize.service.db.DatabaseConstants.DECISION_DEFINITION_INDEX_NAME;
// // import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
// // import static io.camunda.optimize.service.util.ProcessReportDataType.RAW_DATA;
// // import static
// io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
// // import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_DEFINITION_KEY;
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.AbstractPlatformIT;
// // import io.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
// // import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
// // import io.camunda.optimize.dto.optimize.datasource.EngineDataSourceDto;
// // import io.camunda.optimize.dto.optimize.query.IdResponseDto;
// // import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// // import io.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
// // import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// // import io.camunda.optimize.service.util.IdGenerator;
// // import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// // import java.util.Collections;
// // import java.util.List;
// // import org.junit.jupiter.api.BeforeEach;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class PublicApiEntityIdRetrievalIT extends AbstractPlatformIT {
// //   private static final String ACCESS_TOKEN = "secret_export_token";
// //
// //   @BeforeEach
// //   public void before() {
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getOptimizeApiConfiguration()
// //         .setAccessToken(ACCESS_TOKEN);
// //   }
// //
// //   @Test
// //   public void retrieveReportIdsFromCollection() {
// //     // given
// //     createAndSaveDefinitions();
// //     final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();
// //     final String reportId1 = createProcessReport(collectionId);
// //     final String reportId2 = createDecisionReport(collectionId);
// //     dashboardClient.createDashboard(collectionId, Collections.singletonList(reportId1));
// //
// //     final String otherCollection =
// collectionClient.createNewCollectionForAllDefinitionTypes();
// //     final String otherCollReport = createProcessReport(otherCollection);
// //     dashboardClient.createDashboard(collectionId, Collections.singletonList(otherCollReport));
// //
// //     // when
// //     final List<IdResponseDto> reportIds =
// //         publicApiClient.getAllReportIdsInCollection(collectionId, ACCESS_TOKEN);
// //
// //     // then
// //     assertThat(reportIds)
// //         .hasSize(2)
// //         .extracting(IdResponseDto::getId)
// //         .containsExactlyInAnyOrder(reportId1, reportId2);
// //   }
// //
// //   @Test
// //   public void retrieveReportIdsFromNonExistentCollection() {
// //     // when
// //     final List<IdResponseDto> reportIds =
// //         publicApiClient.getAllReportIdsInCollection("fake_id", ACCESS_TOKEN);
// //
// //     // then
// //     assertThat(reportIds).isEmpty();
// //   }
// //
// //   @Test
// //   public void retrieveDashboardIdsFromCollection() {
// //     // given
// //     createAndSaveDefinitions();
// //     final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();
// //     final String reportId1 = createProcessReport(collectionId);
// //     final String reportId2 = createDecisionReport(collectionId);
// //     final String dashboardId1 =
// //         dashboardClient.createDashboard(collectionId, Collections.singletonList(reportId1));
// //     final String dashboardId2 =
// //         dashboardClient.createDashboard(collectionId, List.of(reportId1, reportId2));
// //
// //     final String otherCollection =
// collectionClient.createNewCollectionForAllDefinitionTypes();
// //     final String otherCollReport = createProcessReport(otherCollection);
// //     dashboardClient.createDashboard(otherCollection,
// Collections.singletonList(otherCollReport));
// //
// //     // when
// //     final List<IdResponseDto> reportIds =
// //         publicApiClient.getAllDashboardIdsInCollection(collectionId, ACCESS_TOKEN);
// //
// //     // then
// //     assertThat(reportIds)
// //         .hasSize(2)
// //         .extracting(IdResponseDto::getId)
// //         .containsExactlyInAnyOrder(dashboardId1, dashboardId2);
// //   }
// //
// //   @Test
// //   public void retrieveDashboardIdsFromNonExistentCollection() {
// //     // when
// //     final List<IdResponseDto> reportIds =
// //         publicApiClient.getAllDashboardIdsInCollection("fake_id", ACCESS_TOKEN);
// //
// //     // then
// //     assertThat(reportIds).isEmpty();
// //   }
// //
// //   private void createAndSaveDefinitions() {
// //     final ProcessDefinitionOptimizeDto processDefinition =
// //         ProcessDefinitionOptimizeDto.builder()
// //             .id(IdGenerator.getNextId())
// //             .key(DEFAULT_DEFINITION_KEY)
// //             .name("processDefName")
// //             .version(ALL_VERSIONS)
// //             .dataSource(new EngineDataSourceDto(DEFAULT_ENGINE_ALIAS))
// //             .bpmn20Xml("processXmlString")
// //             .build();
// //     databaseIntegrationTestExtension.addEntryToDatabase(
// //         PROCESS_DEFINITION_INDEX_NAME, processDefinition.getId(), processDefinition);
// //     final DecisionDefinitionOptimizeDto decisionDefinition =
// //         DecisionDefinitionOptimizeDto.builder()
// //             .id(IdGenerator.getNextId())
// //             .key(DEFAULT_DEFINITION_KEY)
// //             .name("decisionDefName")
// //             .version(ALL_VERSIONS)
// //             .dataSource(new EngineDataSourceDto(DEFAULT_ENGINE_ALIAS))
// //             .dmn10Xml("DecisionDef")
// //             .build();
// //     databaseIntegrationTestExtension.addEntryToDatabase(
// //         DECISION_DEFINITION_INDEX_NAME, decisionDefinition.getId(), decisionDefinition);
// //   }
// //
// //   private String createProcessReport(final String collectionId) {
// //     final ProcessReportDataDto reportData =
// //         TemplatedProcessReportDataBuilder.createReportData()
// //             .setProcessDefinitionKey(DEFAULT_DEFINITION_KEY)
// //             .setProcessDefinitionVersion(ALL_VERSIONS)
// //             .setReportDataType(RAW_DATA)
// //             .build();
// //     return reportClient.createSingleProcessReport(reportData, collectionId);
// //   }
// //
// //   private String createDecisionReport(final String collectionId) {
// //     final DecisionReportDataDto reportData = new DecisionReportDataDto();
// //     reportData.setDefinitions(
// //         List.of(
// //             new ReportDataDefinitionDto(
// //                 DEFAULT_DEFINITION_KEY, DEFAULT_DEFINITION_KEY, List.of(ALL_VERSIONS))));
// //     return reportClient.createSingleDecisionReport(reportData, collectionId);
// //   }
// // }
