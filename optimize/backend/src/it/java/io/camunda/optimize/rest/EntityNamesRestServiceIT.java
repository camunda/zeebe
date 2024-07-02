/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.dashboard.ManagementDashboardService.CURRENTLY_IN_PROGRESS_NAME_LOCALIZATION_CODE;
import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static io.camunda.optimize.util.SuppressionConstants.SAME_PARAM_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.ReportType;
import io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityNameRequestDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityNameResponseDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.EventScopeType;
import io.camunda.optimize.dto.optimize.query.event.process.source.ExternalEventSourceConfigDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.ExternalEventSourceEntryDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.rest.EventProcessMappingCreateRequestDto;
import io.camunda.optimize.exception.OptimizeIntegrationTestException;
import io.camunda.optimize.service.dashboard.InstantPreviewDashboardService;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class EntityNamesRestServiceIT extends AbstractEntitiesRestServiceIT {

  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  @Test
  public void getEntityNames_WorksForAllPossibleEntities() {
    // given
    String reportId = addSingleReportToOptimize("aReportName", ReportType.PROCESS);
    String dashboardId = addDashboardToOptimize("aDashboardName");
    String collectionId = addCollection("aCollectionName");
    String eventProcessId = addEventProcessMappingToOptimize("anEventProcessName");
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameResponseDto result =
        entitiesClient.getEntityNames(collectionId, dashboardId, reportId, eventProcessId);

    // then
    assertThat(result.getCollectionName()).isEqualTo("aCollectionName");
    assertThat(result.getDashboardName()).isEqualTo("aDashboardName");
    assertThat(result.getReportName()).isEqualTo("aReportName");
    assertThat(result.getEventBasedProcessName()).isEqualTo("anEventProcessName");
  }

  @Test
  public void getEntityNames_ReturnsNoResponseForEventBasedProcessIfThereIsNone() {
    // given
    String reportId = addSingleReportToOptimize("aReportName", ReportType.PROCESS);
    String dashboardId = addDashboardToOptimize("aDashboardName");
    String collectionId = addCollection("aCollectionName");
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameResponseDto result =
        entitiesClient.getEntityNames(collectionId, dashboardId, reportId, "eventProcessId");

    // then
    assertThat(result.getCollectionName()).isEqualTo("aCollectionName");
    assertThat(result.getDashboardName()).isEqualTo("aDashboardName");
    assertThat(result.getReportName()).isEqualTo("aReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_SeveralReportsDoNotDistortResult() {
    // given
    String reportId = addSingleReportToOptimize("aProcessReportName", ReportType.PROCESS);
    addSingleReportToOptimize("aDecisionReportName", ReportType.DECISION);
    addCombinedReport("aCombinedReportName");
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameResponseDto result = entitiesClient.getEntityNames(null, null, reportId, null);

    // then
    assertThat(result.getCollectionName()).isNull();
    assertThat(result.getDashboardName()).isNull();
    assertThat(result.getReportName()).isEqualTo("aProcessReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_WorksForDecisionReports() {
    // given
    String reportId = addSingleReportToOptimize("aDecisionReportName", ReportType.DECISION);
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameResponseDto result = entitiesClient.getEntityNames(null, null, reportId, null);

    // then
    assertThat(result.getCollectionName()).isNull();
    assertThat(result.getDashboardName()).isNull();
    assertThat(result.getReportName()).isEqualTo("aDecisionReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_WorksForCombinedReports() {
    // given
    String reportId = addCombinedReport("aCombinedReportName");
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameResponseDto result = entitiesClient.getEntityNames(null, null, reportId, null);

    // then
    assertThat(result.getCollectionName()).isNull();
    assertThat(result.getDashboardName()).isNull();
    assertThat(result.getReportName()).isEqualTo("aCombinedReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Tag(OPENSEARCH_SHOULD_BE_PASSING)
  @Test
  public void getEntityNames_NotAvailableIdReturns404() {
    // given
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetEntityNamesRequest(
                new EntityNameRequestDto(null, null, "notAvailableRequest", null))
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Tag(OPENSEARCH_SHOULD_BE_PASSING)
  @Test
  public void getEntityNames_NoIdProvidedReturns400() {
    // given
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetEntityNamesRequest(new EntityNameRequestDto(null, null, null, null))
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("templatesAndExpectedLocalizedNames")
  public void getEntityNames_localizesInstantPreviewDashboardContent(
      final String template, final String locale, final String expectedDashboardName) {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    String processDefKey = "dummy";
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(processDefKey));
    importAllEngineEntitiesFromScratch();
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(processDefKey, template);
    assertThat(instantPreviewDashboard).isPresent();

    // when
    final EntityNameResponseDto result =
        entitiesClient.getEntityNamesLocalized(
            null, instantPreviewDashboard.get().getDashboardId(), null, null, locale);

    // then
    assertThat(result.getDashboardName()).isEqualTo(expectedDashboardName);
  }

  @ParameterizedTest
  @MethodSource("localizedReportName")
  public void managementReportNamesAreLocalized(
      final String locale, final String expectedReportName) {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("aProcess"));
    importAllEngineEntitiesFromScratch();
    embeddedOptimizeExtension.getManagementDashboardService().init();

    // when
    final EntityNameResponseDto result =
        entitiesClient.getEntityNamesLocalized(
            null, null, getIdForManagementReport(), null, locale);

    // then
    assertThat(result.getReportName()).isEqualTo(expectedReportName);
  }

  @SuppressWarnings(SAME_PARAM_VALUE)
  private String addEventProcessMappingToOptimize(final String eventProcessName) {
    EventProcessMappingCreateRequestDto eventBasedProcessDto =
        EventProcessMappingCreateRequestDto.eventProcessMappingCreateBuilder()
            .name(eventProcessName)
            .eventSources(
                Collections.singletonList(
                    ExternalEventSourceEntryDto.builder()
                        .configuration(
                            ExternalEventSourceConfigDto.builder()
                                .includeAllGroups(true)
                                .eventScope(Collections.singletonList(EventScopeType.ALL))
                                .build())
                        .build()))
            .build();
    return eventProcessClient.createEventProcessMapping(eventBasedProcessDto);
  }

  private static Stream<Arguments> localizedReportName() {
    return Stream.of(
        Arguments.of("en", "Currently in progress"),
        Arguments.of("de", "Aktuell laufende Prozesse"));
  }

  private static Stream<Arguments> templatesAndExpectedLocalizedNames() {
    return Stream.of(
        Arguments.of("template1.json", "en", "Instant process dashboard"),
        Arguments.of("template1.json", "de", "Instant Prozessübersicht"),
        Arguments.of("template2.json", "en", "KPI dashboard"),
        Arguments.of("template2.json", "de", "KPI dashboard"));
  }

  private String getIdForManagementReport() {
    return databaseIntegrationTestExtension
        .getAllDocumentsOfIndexAs(
            SINGLE_PROCESS_REPORT_INDEX_NAME, SingleProcessReportDefinitionRequestDto.class)
        .stream()
        .filter(report -> report.getName().equals(CURRENTLY_IN_PROGRESS_NAME_LOCALIZATION_CODE))
        .findFirst()
        .map(SingleProcessReportDefinitionRequestDto::getId)
        .orElseThrow(
            () -> new OptimizeIntegrationTestException("Cannot find any management reports"));
  }
}
