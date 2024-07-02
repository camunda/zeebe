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
// // import static
// io.camunda.optimize.service.db.DatabaseConstants.SINGLE_DECISION_REPORT_INDEX_NAME;
// // import static
// io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
// // import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.AbstractPlatformIT;
// // import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
// // import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
// // import io.camunda.optimize.exception.OptimizeIntegrationTestException;
// // import jakarta.ws.rs.core.Response;
// // import java.util.Optional;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class PublicApiReportDeletionIT extends AbstractPlatformIT {
// //   private static final String ACCESS_TOKEN = "secret_export_token";
// //
// //   @Test
// //   public void deleteProcessReport() {
// //     // given
// //     setAccessToken();
// //     final String reportId = reportClient.createEmptySingleProcessReport();
// //
// //     // when
// //     final Response deleteResponse = publicApiClient.deleteReport(reportId, ACCESS_TOKEN);
// //
// //     // then
// //
// assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //     assertThat(
// //
// databaseIntegrationTestExtension.getDocumentCountOf(SINGLE_PROCESS_REPORT_INDEX_NAME))
// //         .isEqualTo(0);
// //   }
// //
// //   @Test
// //   public void deleteManagementProcessReportNotSupported() {
// //     // given
// //     setAccessToken();
// //     embeddedOptimizeExtension.getManagementDashboardService().init();
// //     final String reportId = findManagementReportId();
// //
// //     // when
// //     final Response deleteResponse = publicApiClient.deleteReport(reportId, ACCESS_TOKEN);
// //
// //     // then
// //
// assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void deleteInstantPreviewProcessReportNotSupported() {
// //     // given
// //     setAccessToken();
// //     final String processDefKey = "dummy";
// //     engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(processDefKey));
// //     importAllEngineEntitiesFromScratch();
// //     DashboardDefinitionRestDto originalDashboard =
// //         dashboardClient.getInstantPreviewDashboard(processDefKey, "template1.json");
// //     final Optional<String> instantReportId =
// originalDashboard.getTileIds().stream().findFirst();
// //     assertThat(instantReportId).isPresent();
// //
// //     // when
// //     final Response deleteResponse =
// //         publicApiClient.deleteReport(instantReportId.get(), ACCESS_TOKEN);
// //
// //     // then
// //
// assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void deleteDecisionReport() {
// //     // given
// //     setAccessToken();
// //     final String reportId = reportClient.createEmptySingleDecisionReport();
// //
// //     // when
// //     final Response deleteResponse = publicApiClient.deleteReport(reportId, ACCESS_TOKEN);
// //
// //     // then
// //
// assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //     assertThat(
// //
// databaseIntegrationTestExtension.getDocumentCountOf(SINGLE_DECISION_REPORT_INDEX_NAME))
// //         .isEqualTo(0);
// //   }
// //
// //   @Test
// //   public void deleteReportNotExisting() {
// //     // given
// //     setAccessToken();
// //
// //     // when
// //     final Response deleteResponse = publicApiClient.deleteReport("notExisting", ACCESS_TOKEN);
// //
// //     // then
// //
// assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
// //   }
// //
// //   private void setAccessToken() {
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getOptimizeApiConfiguration()
// //         .setAccessToken(ACCESS_TOKEN);
// //   }
// //
// //   private String findManagementReportId() {
// //     return databaseIntegrationTestExtension
// //         .getAllDocumentsOfIndexAs(
// //             SINGLE_PROCESS_REPORT_INDEX_NAME, SingleProcessReportDefinitionRequestDto.class)
// //         .stream()
// //         .filter(reportDef -> reportDef.getData().isManagementReport())
// //         .findFirst()
// //         .map(ReportDefinitionDto::getId)
// //         .orElseThrow(() -> new OptimizeIntegrationTestException("No Management Report
// Found"));
// //   }
// // }
