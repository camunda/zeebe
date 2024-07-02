/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_INDEX_NAME;
import static io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardReportTileDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.sharing.DashboardShareRestDto;
import io.camunda.optimize.dto.optimize.query.sharing.ReportShareRestDto;
import io.camunda.optimize.exception.OptimizeIntegrationTestException;
import io.camunda.optimize.service.dashboard.InstantPreviewDashboardService;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.ScriptData;
import io.camunda.optimize.service.sharing.AbstractSharingIT;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class SharingRestServiceIT extends AbstractSharingIT {

  @Test
  public void checkShareStatusWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCheckSharingStatusRequest(null)
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void createNewReportShareWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildShareReportRequest(null)
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void createNewDashboardShareWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildShareDashboardRequest(null)
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void createNewReportShare() {
    // given
    String reportId = createReportWithInstance();
    ReportShareRestDto share = createReportShare(reportId);

    // when
    Response response = sharingClient.createReportShareResponse(share);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    String id = response.readEntity(String.class);
    assertThat(id).isNotNull();
  }

  @Test
  public void createNewReportShareWithSharingDisabled() {
    // given
    String reportId = createReportWithInstance();
    embeddedOptimizeExtension.getConfigurationService().setSharingEnabled(false);
    ReportShareRestDto share = createReportShare(reportId);

    // when
    Response response = sharingClient.createReportShareResponse(share);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void createNewReportShareForManagementReport() {
    // given
    String reportId = createManagementReport();
    ReportShareRestDto share = createReportShare(reportId);

    // when
    Response response = sharingClient.createReportShareResponse(share);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void createNewDashboardShare() {
    // given
    String dashboard = addEmptyDashboardToOptimize();

    DashboardShareRestDto sharingDto = new DashboardShareRestDto();
    sharingDto.setDashboardId(dashboard);

    // when
    Response response = sharingClient.createDashboardShareResponse(sharingDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    String id = response.readEntity(String.class);
    assertThat(id).isNotNull();
  }

  @Test
  public void createNewDashboardShareForManagementDashboard() {
    // given
    String dashboardId = createManagementDashboard();
    final DashboardShareRestDto dashboardShareDto = createDashboardShareDto(dashboardId);

    // when
    Response response = sharingClient.createDashboardShareResponse(dashboardShareDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void createNewDashboardShareForInstantPreviewDashboard() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    String processDefKey = "dummy";
    String dashboardJsonTemplateFilename = "template2.json";
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(processDefKey));
    importAllEngineEntitiesFromScratch();
    InstantDashboardDataDto instantPreviewDashboard =
        instantPreviewDashboardService
            .createInstantPreviewDashboard(processDefKey, dashboardJsonTemplateFilename)
            .orElseThrow(
                () -> new OptimizeIntegrationTestException("Could not get instant dashboard"));
    final DashboardShareRestDto dashboardShareDto =
        createDashboardShareDto(instantPreviewDashboard.getDashboardId());

    // when
    Response response = sharingClient.createDashboardShareResponse(dashboardShareDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void createNewReportShareForInstantPreviewDashboardReport() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    String processDefKey = "dummy";
    String dashboardJsonTemplateFilename = "template2.json";
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(processDefKey));
    importAllEngineEntitiesFromScratch();
    InstantDashboardDataDto instantPreviewDashboard =
        instantPreviewDashboardService
            .createInstantPreviewDashboard(processDefKey, dashboardJsonTemplateFilename)
            .orElseThrow(
                () -> new OptimizeIntegrationTestException("Could not get instant dashboard"));
    final DashboardDefinitionRestDto dashboard =
        dashboardClient.getDashboard(instantPreviewDashboard.getDashboardId());
    final DashboardReportTileDto instantPreviewReport = dashboard.getTiles().get(0);
    final ReportShareRestDto reportShare = createReportShare(instantPreviewReport.getId());

    // when
    Response response = sharingClient.createReportShareResponse(reportShare);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void deleteReportShareWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteReportShareRequest("1124")
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void deleteNonExistingReportShare() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteReportShareRequest("nonExistingId")
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void deleteDashboardShareWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteDashboardShareRequest("1124")
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void deleteNonExistingDashboardShare() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteDashboardShareRequest("nonExistingId")
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void deleteReportShare() {
    // given
    String reportId = createReportWithInstance();
    String id = addShareForReport(reportId);

    // when
    Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildDeleteReportShareRequest(id).execute();

    // then the status code is okay
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThat(getShareForReport(FAKE_REPORT_ID)).isNull();
  }

  @Test
  public void deleteDashboardShare() {
    // given
    String reportId = createReportWithInstance();
    String dashboardWithReport = createDashboardWithReport(reportId);
    String id = addShareForDashboard(dashboardWithReport);

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteDashboardShareRequest(id)
            .execute();

    // then the status code is okay
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThat(getShareForReport(reportId)).isNull();
  }

  @Test
  public void findShareForReport() {
    // given
    String reportId = createReportWithInstance();
    String id = addShareForReport(reportId);

    // when
    ReportShareRestDto share = getShareForReport(reportId);

    // then
    assertThat(share).isNotNull();
    assertThat(share.getId()).isEqualTo(id);
  }

  @Test
  public void findShareForReportWithoutAuthentication() {
    // given
    addShareForFakeReport();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildFindShareForReportRequest(FAKE_REPORT_ID)
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void findShareForSharedDashboard() {
    // given
    String reportId = createReportWithInstance();
    String dashboardWithReport = createDashboardWithReport(reportId);
    String id = addShareForDashboard(dashboardWithReport);

    // when
    DashboardShareRestDto share =
        findShareForDashboard(dashboardWithReport).readEntity(DashboardShareRestDto.class);

    // then
    assertThat(share).isNotNull();
    assertThat(share.getId()).isEqualTo(id);
  }

  @Test
  public void evaluateSharedDashboard() {
    // given
    String reportId = createReportWithInstance();
    String dashboardId = createDashboardWithReport(reportId);
    String dashboardShareId = addShareForDashboard(dashboardId);

    // when
    DashboardDefinitionRestDto dashboardShareDto =
        sharingClient.evaluateDashboard(dashboardShareId);

    // then
    assertThat(dashboardShareDto).isNotNull();
    assertThat(dashboardShareDto.getId()).isEqualTo(dashboardId);
    assertThat(dashboardShareDto.getTiles()).isNotNull();
    assertThat(dashboardShareDto.getTiles()).hasSize(1);

    // when
    String reportShareId = dashboardShareDto.getTiles().get(0).getId();
    HashMap<?, ?> evaluatedReportAsMap =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildEvaluateSharedDashboardReportRequest(dashboardShareId, reportShareId)
            .execute(HashMap.class, Response.Status.OK.getStatusCode());

    // then
    assertReportData(reportId, evaluatedReportAsMap);
  }

  @Test
  public void findShareForDashboardWithoutAuthentication() {
    // given
    String reportId = createReportWithInstance();
    String dashboardWithReport = createDashboardWithReport(reportId);
    addShareForDashboard(dashboardWithReport);

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildFindShareForDashboardRequest(dashboardWithReport)
            .withoutAuthentication()
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void evaluationOfNotExistingShareReturnsError() {

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildEvaluateSharedReportRequest("123")
            .execute();

    // then
    assertThat(response.getStatus())
        .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  public void checkSharingAuthorizationWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDashboardShareAuthorizationCheck("1124")
            .withoutAuthentication()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void checkSharingAuthorizationIsOkay() {
    // given
    String reportId = createReportWithInstance();
    String dashboardId = createDashboardWithReport(reportId);

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDashboardShareAuthorizationCheck(dashboardId)
            .execute();

    // then the status code is okay
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void checkSharingAuthorizationResultsInForbidden() {
    // given
    engineIntegrationExtension.addUser("kermit", "kermit");
    engineIntegrationExtension.grantUserOptimizeAccess("kermit");
    String reportId = createReportWithInstance();
    String dashboardId = createDashboardWithReport(reportId);

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDashboardShareAuthorizationCheck(dashboardId)
            .withUserAuthentication("kermit", "kermit")
            .execute();

    // then the status code is okay
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  private Response findShareForDashboard(String dashboardId) {
    return embeddedOptimizeExtension
        .getRequestExecutor()
        .buildFindShareForDashboardRequest(dashboardId)
        .execute();
  }

  private void addShareForFakeReport() {
    addShareForReport(FAKE_REPORT_ID);
  }

  @SneakyThrows
  private String createManagementReport() {
    // The initial report is created for a specific process
    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey("procDefKey")
            .setProcessDefinitionVersion("1")
            .setReportDataType(PROC_INST_FREQ_GROUP_BY_NONE)
            .build();
    final String reportId =
        reportClient.createSingleProcessReport(
            new SingleProcessReportDefinitionRequestDto(reportData));
    databaseIntegrationTestExtension.update(
        DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME,
        reportId,
        new ScriptData(Collections.emptyMap(), "ctx._source.data.managementReport = true"));
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    return reportId;
  }

  @SneakyThrows
  protected String createManagementDashboard() {
    final String dashboardId = dashboardClient.createEmptyDashboard();
    databaseIntegrationTestExtension.update(
        DASHBOARD_INDEX_NAME,
        dashboardId,
        new ScriptData(Collections.emptyMap(), "ctx._source.managementDashboard = true"));
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    return dashboardId;
  }
}
