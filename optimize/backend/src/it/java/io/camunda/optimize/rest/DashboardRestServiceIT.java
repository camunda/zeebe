/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestUtil.getOffsetDiffInHours;
import static io.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_TIMEZONE;
import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_SHARE_INDEX_NAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static jakarta.ws.rs.HttpMethod.DELETE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.dto.optimize.query.IdResponseDto;
import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.sharing.DashboardShareRestDto;
import io.camunda.optimize.service.dashboard.ManagementDashboardService;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Collections;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

@Tag(OPENSEARCH_PASSING)
public class DashboardRestServiceIT extends AbstractDashboardRestServiceIT {

  @Test
  public void copyPrivateDashboard() {
    // given
    String dashboardId = dashboardClient.createEmptyDashboard(null);
    createEmptyReportToDashboard(dashboardId);

    // when
    IdResponseDto copyId = dashboardClient.copyDashboard(dashboardId);

    // then
    DashboardDefinitionRestDto oldDashboard = dashboardClient.getDashboard(dashboardId);
    DashboardDefinitionRestDto dashboard = dashboardClient.getDashboard(copyId.getId());
    assertThat(dashboard).hasToString(oldDashboard.toString());
    assertThat(dashboard.getName()).isEqualTo(oldDashboard.getName() + " – Copy");
    assertThat(dashboard.getTiles()).containsExactlyElementsOf(oldDashboard.getTiles());
  }

  @Test
  public void copyPrivateDashboardWithDescription() {
    // given
    DashboardDefinitionRestDto definitionDto = new DashboardDefinitionRestDto();
    definitionDto.setDescription("dashboard description");
    final String dashboardId = dashboardClient.createDashboard(definitionDto);

    // when
    IdResponseDto copyId = dashboardClient.copyDashboard(dashboardId);

    // then
    DashboardDefinitionRestDto oldDashboard = dashboardClient.getDashboard(dashboardId);
    DashboardDefinitionRestDto dashboard = dashboardClient.getDashboard(copyId.getId());
    assertThat(dashboard).hasToString(oldDashboard.toString());
    assertThat(dashboard.getDescription()).isEqualTo(oldDashboard.getDescription());
  }

  @Test
  public void copyPrivateManagementDashboardNotSupported() {
    // given
    embeddedOptimizeExtension.getManagementDashboardService().init();

    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCopyDashboardRequest(ManagementDashboardService.MANAGEMENT_DASHBOARD_ID, null)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void copyPrivateManagementDashboardIntoCollectionNotSupported() {
    // given
    embeddedOptimizeExtension.getManagementDashboardService().init();
    final String collectionId = collectionClient.createNewCollection();

    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCopyDashboardRequest(
                ManagementDashboardService.MANAGEMENT_DASHBOARD_ID, collectionId)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void copyPrivateDashboardWithNameParameter() {
    // given
    final String dashboardId = dashboardClient.createEmptyDashboard(null);
    createEmptyReportToDashboard(dashboardId);

    final String testDashboardCopyName = "This is my new report copy! ;-)";

    // when
    IdResponseDto copyId =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCopyDashboardRequest(dashboardId)
            .addSingleQueryParam("name", testDashboardCopyName)
            .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());

    // then
    DashboardDefinitionRestDto oldDashboard = dashboardClient.getDashboard(dashboardId);
    DashboardDefinitionRestDto dashboard = dashboardClient.getDashboard(copyId.getId());
    assertThat(dashboard).hasToString(oldDashboard.toString());
    assertThat(dashboard.getName()).isEqualTo(testDashboardCopyName);
  }

  @Test
  public void getDashboardWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withoutAuthentication()
            .buildGetDashboardRequest("asdf")
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void getDashboard() {
    // given
    DashboardDefinitionRestDto definitionDto = generateDashboardDefinitionDto();
    String id = dashboardClient.createDashboard(definitionDto);

    // when
    DashboardDefinitionRestDto returnedDashboard = dashboardClient.getDashboard(id);

    // then
    assertThat(returnedDashboard).isNotNull();
    assertThat(returnedDashboard.getId()).isEqualTo(id);
    assertThat(returnedDashboard.getName()).isEqualTo(definitionDto.getName());
    assertThat(returnedDashboard.getOwner()).isEqualTo(DEFAULT_FULLNAME);
    assertThat(returnedDashboard.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
  }

  @Test
  public void getDashboard_adoptTimezoneFromHeader() {
    // given
    OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();
    generateDashboardDefinitionDto();
    String dashboardId = dashboardClient.createDashboard(generateDashboardDefinitionDto());

    // when
    DashboardDefinitionRestDto returnedDashboard =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetDashboardRequest(dashboardId)
            .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
            .execute(DashboardDefinitionRestDto.class, Response.Status.OK.getStatusCode());

    // then
    assertThat(returnedDashboard).isNotNull();
    assertThat(returnedDashboard.getCreated()).isEqualTo(now);
    assertThat(returnedDashboard.getLastModified()).isEqualTo(now);
    assertThat(getOffsetDiffInHours(returnedDashboard.getCreated(), now)).isEqualTo(1.);
    assertThat(getOffsetDiffInHours(returnedDashboard.getLastModified(), now)).isEqualTo(1.);
  }

  @Test
  public void getDashboardForNonExistingIdThrowsError() {
    // when
    String response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetDashboardRequest("fooid")
            .execute(String.class, Response.Status.NOT_FOUND.getStatusCode());

    // then
    assertThat(response).containsSequence("Dashboard does not exist!");
  }

  @Test
  public void deleteDashboardWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withoutAuthentication()
            .buildDeleteDashboardRequest("1124")
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void deleteNewDashboard() {
    // given
    String id = dashboardClient.createEmptyDashboard(null);

    // when
    Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildDeleteDashboardRequest(id).execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  public void deleteManagementDashboardNotSupported() {
    // given
    embeddedOptimizeExtension.getManagementDashboardService().init();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteDashboardRequest(ManagementDashboardService.MANAGEMENT_DASHBOARD_ID)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void deleteNonExistingDashboard() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteDashboardRequest("nonExistingId")
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void deleteDashboardWithShares_shareAlsoGetsDeleted() {
    // given
    final String dashboardId = dashboardClient.createDashboard(generateDashboardDefinitionDto());
    final String shareId = dashboardClient.createDashboardShareForDashboard(dashboardId);

    // then
    assertThat(documentShareExists(shareId)).isTrue();

    // when
    dashboardClient.deleteDashboard(dashboardId);

    // then
    assertThat(documentShareExists(shareId)).isFalse();
  }

  @Test
  public void deleteDashboardWithShares_shareGetsDeleted_despiteDashboardDeleteFail() {
    // given
    final ClientAndServer dbMockServer = useAndGetDbMockServer();

    final String dashboardId = dashboardClient.createDashboard(generateDashboardDefinitionDto());
    final HttpRequest requestMatcher =
        request()
            .withPath("/.*-" + DASHBOARD_INDEX_NAME + "/_doc/" + dashboardId)
            .withMethod(DELETE);
    dbMockServer
        .when(requestMatcher, Times.once())
        .error(HttpError.error().withDropConnection(true));

    final String shareId = dashboardClient.createDashboardShareForDashboard(dashboardId);

    // then
    assertThat(documentShareExists(shareId)).isTrue();

    // when
    embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDeleteDashboardRequest(dashboardId)
        .execute(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    assertThat(dashboardClient.getDashboard(dashboardId)).isNotNull();
    assertThat(documentShareExists(shareId)).isFalse();
  }

  @Test
  public void deleteDashboardWithShares_shareDeleteFails_dashboardNotDeleted() {
    // given
    final ClientAndServer dbMockServer = useAndGetDbMockServer();

    final String dashboardId = dashboardClient.createDashboard(generateDashboardDefinitionDto());
    final String shareId = dashboardClient.createDashboardShareForDashboard(dashboardId);
    final HttpRequest requestMatcher =
        request()
            .withPath("/.*-" + DASHBOARD_SHARE_INDEX_NAME + "/_doc/" + shareId)
            .withMethod(DELETE);
    dbMockServer
        .when(requestMatcher, Times.once())
        .error(HttpError.error().withDropConnection(true));

    // then
    assertThat(documentShareExists(shareId)).isTrue();

    // when
    embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDeleteDashboardRequest(dashboardId)
        .execute(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    assertThat(dashboardClient.getDashboard(dashboardId)).isNotNull();
    assertThat(documentShareExists(shareId)).isTrue();
  }

  @SneakyThrows
  private boolean documentShareExists(final String shareId) {
    return databaseIntegrationTestExtension
        .getAllDocumentsOfIndexAs(DASHBOARD_SHARE_INDEX_NAME, DashboardShareRestDto.class)
        .stream()
        .anyMatch(dashboardShare -> dashboardShare.getId().equals(shareId));
  }

  private void createEmptyReportToDashboard(final String dashboardId) {
    final String reportId = reportClient.createEmptySingleProcessReportInCollection(null);
    dashboardClient.updateDashboardWithReports(dashboardId, Collections.singletonList(reportId));
  }
}
