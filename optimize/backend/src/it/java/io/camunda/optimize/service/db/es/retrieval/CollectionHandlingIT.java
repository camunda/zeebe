/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.retrieval;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.DefinitionType.DECISION;
import static io.camunda.optimize.dto.optimize.DefinitionType.PROCESS;
import static io.camunda.optimize.service.db.DatabaseConstants.ALERT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.COLLECTION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.TENANT_INDEX_NAME;
import static io.camunda.optimize.service.db.writer.CollectionWriter.DEFAULT_COLLECTION_NAME;
import static io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_DEFINITION_KEY;
import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_TENANTS;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;

import com.google.common.collect.Lists;
import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.ReportType;
import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.TenantDto;
import io.camunda.optimize.dto.optimize.query.alert.AlertCreationRequestDto;
import io.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDataDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleRequestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryUpdateDto;
import io.camunda.optimize.dto.optimize.query.collection.PartialCollectionDataDto;
import io.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardReportTileDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardTileType;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DimensionDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.PositionDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityType;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.SingleReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionResponseDto;
import io.camunda.optimize.dto.optimize.rest.collection.CollectionScopeEntryResponseDto;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.util.BpmnModels;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

@Tag(OPENSEARCH_PASSING)
public class CollectionHandlingIT extends AbstractPlatformIT {

  private static Stream<DefinitionType> definitionTypes() {
    return Stream.of(PROCESS, DECISION);
  }

  @Test
  public void collectionIsWrittenToDatabase() {
    // given
    String id = collectionClient.createNewCollection();

    // when
    Optional<CollectionDefinitionDto> result =
        databaseIntegrationTestExtension.getDatabaseEntryById(
            COLLECTION_INDEX_NAME, id, CollectionDefinitionDto.class);

    // then
    assertThat(result).isPresent();
  }

  @Test
  public void newCollectionIsCorrectlyInitialized() {
    // given
    String id = collectionClient.createNewCollection();

    // when
    CollectionDefinitionRestDto collection = collectionClient.getCollectionById(id);
    List<EntityResponseDto> collectionEntities = collectionClient.getEntitiesForCollection(id);

    // then
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collection.getName()).isEqualTo(DEFAULT_COLLECTION_NAME);
    assertThat(collectionEntities).isEmpty();
    assertThat(collection.getData().getConfiguration()).isNotNull();
  }

  @Test
  public void getResolvedCollection() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String dashboardId = dashboardClient.createEmptyDashboard(collectionId);
    final String reportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);

    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    CollectionDefinitionRestDto collection = collectionClient.getCollectionById(collectionId);
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    assertThat(collection).isNotNull();
    assertThat(collection.getId()).isEqualTo(collectionId);
    assertThat(collectionEntities).hasSize(2);
    assertThat(
            collectionEntities.stream().map(EntityResponseDto::getId).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(dashboardId, reportId);
  }

  @Test
  public void getResolvedCollectionContainsCombinedReportSubEntityCounts() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String reportId1 = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    final String reportId2 = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    final String combinedReportId = reportClient.createEmptyCombinedReport(collectionId);

    reportClient.updateCombinedReport(combinedReportId, Lists.newArrayList(reportId1, reportId2));

    // when
    CollectionDefinitionRestDto collection = collectionClient.getCollectionById(collectionId);
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    assertThat(collection).isNotNull();
    assertThat(collection.getId()).isEqualTo(collectionId);
    assertThat(collectionEntities).hasSize(3);
    final EntityResponseDto combinedReportEntityDto =
        collectionEntities.stream().filter(EntityResponseDto::getCombined).findFirst().get();
    assertThat(combinedReportEntityDto.getData().getSubEntityCounts()).hasSize(1);
    assertThat(combinedReportEntityDto.getData().getSubEntityCounts().get(EntityType.REPORT))
        .isEqualTo(2L);
  }

  @Test
  public void updateCollection() {
    // given
    String id = collectionClient.createNewCollection();
    OffsetDateTime now = OffsetDateTime.parse("2019-04-23T18:00:00+01:00");
    LocalDateUtil.setCurrentTime(now);

    PartialCollectionDefinitionRequestDto collectionUpdate =
        new PartialCollectionDefinitionRequestDto();
    collectionUpdate.setName("MyCollection");
    final Map<String, String> configuration = Collections.singletonMap("Foo", "Bar");
    final PartialCollectionDataDto data = new PartialCollectionDataDto();
    data.setConfiguration(configuration);
    collectionUpdate.setData(data);

    // when
    collectionClient.updateCollection(id, collectionUpdate);
    CollectionDefinitionRestDto collection = collectionClient.getCollectionById(id);
    List<EntityResponseDto> collectionEntities = collectionClient.getEntitiesForCollection(id);

    // then
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collection.getName()).isEqualTo("MyCollection");
    assertThat(collection.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
    assertThat(collection.getLastModified()).isEqualTo(now);
    assertThat(collection.getData().getConfiguration()).isEqualTo(configuration);
    assertThat(collectionEntities).isEmpty();
  }

  @Test
  public void updatePartialCollection() {
    // given
    String id = collectionClient.createNewCollection();
    OffsetDateTime now = OffsetDateTime.parse("2019-04-23T18:00:00+01:00");
    LocalDateUtil.setCurrentTime(now);

    // when (update only name)
    PartialCollectionDefinitionRequestDto collectionUpdate =
        new PartialCollectionDefinitionRequestDto();
    collectionUpdate.setName("MyCollection");

    collectionClient.updateCollection(id, collectionUpdate);
    CollectionDefinitionRestDto collection = collectionClient.getCollectionById(id);

    // then
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collection.getName()).isEqualTo("MyCollection");
    assertThat(collection.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
    assertThat(collection.getLastModified()).isEqualTo(now);

    // when (update only configuration)
    collectionUpdate = new PartialCollectionDefinitionRequestDto();
    final Map<String, String> configuration = Collections.singletonMap("Foo", "Bar");
    PartialCollectionDataDto data = new PartialCollectionDataDto();
    data.setConfiguration(configuration);
    collectionUpdate.setData(data);

    collectionClient.updateCollection(id, collectionUpdate);
    collection = collectionClient.getCollectionById(id);

    // then
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collection.getName()).isEqualTo("MyCollection");
    assertThat(collection.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
    assertThat(collection.getLastModified()).isEqualTo(now);
    CollectionDataDto resultCollectionData = collection.getData();
    assertThat(resultCollectionData.getConfiguration()).isEqualTo(configuration);

    // when (again only update name)
    collectionUpdate = new PartialCollectionDefinitionRequestDto();
    collectionUpdate.setName("TestNewCollection");

    collectionClient.updateCollection(id, collectionUpdate);
    collection = collectionClient.getCollectionById(id);

    // then
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collection.getName()).isEqualTo("TestNewCollection");
    assertThat(collection.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
    assertThat(collection.getLastModified()).isEqualTo(now);
    resultCollectionData = collection.getData();
    assertThat(resultCollectionData.getConfiguration()).isEqualTo(configuration);
  }

  @Test
  public void singleProcessReportCanBeCreatedInsideCollection() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String reportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);

    // when
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    EntityResponseDto report = collectionEntities.get(0);
    assertThat(report.getId()).isEqualTo(reportId);
    assertThat(report.getEntityType()).isEqualTo(EntityType.REPORT);
    assertThat(report.getReportType()).isEqualTo(ReportType.PROCESS);
    assertThat(report.getCombined()).isEqualTo(false);
  }

  @Test
  public void singleDecisionReportCanBeCreatedInsideCollection() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String reportId = reportClient.createEmptySingleDecisionReportInCollection(collectionId);

    // when
    List<EntityResponseDto> copiedCollectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    EntityResponseDto report = copiedCollectionEntities.get(0);
    assertThat(report.getId()).isEqualTo(reportId);
    assertThat(report.getEntityType()).isEqualTo(EntityType.REPORT);
    assertThat(report.getReportType()).isEqualTo(ReportType.DECISION);
    assertThat(report.getCombined()).isEqualTo(false);
  }

  @Test
  public void combinedProcessReportCanBeCreatedInsideCollection() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String reportId = reportClient.createEmptyCombinedReport(collectionId);

    // when
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    EntityResponseDto report = collectionEntities.get(0);
    assertThat(report.getId()).isEqualTo(reportId);
    assertThat(report.getEntityType()).isEqualTo(EntityType.REPORT);
    assertThat(report.getReportType()).isEqualTo(ReportType.PROCESS);
    assertThat(report.getCombined()).isEqualTo(true);
  }

  @Test
  public void dashboardCanBeCreatedInsideCollection() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String dashboardId = dashboardClient.createEmptyDashboard(collectionId);

    // when
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    EntityResponseDto dashboard = collectionEntities.get(0);
    assertThat(dashboard.getId()).isEqualTo(dashboardId);
    assertThat(dashboard.getEntityType()).isEqualTo(EntityType.DASHBOARD);
    assertThat(dashboard.getReportType()).isNull();
    assertThat(dashboard.getCombined()).isNull();
  }

  @Test
  public void singleProcessReportCanNotBeCreatedForInvalidCollection() {
    // given
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCreateSingleProcessReportRequest(singleProcessReportDefinitionDto)
            .execute();

    // then
    assertThat(createResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void singleDecisionReportCanNotBeCreatedForInvalidCollection() {
    // given
    SingleDecisionReportDefinitionRequestDto singleDecisionReportDefinitionDto =
        new SingleDecisionReportDefinitionRequestDto();
    singleDecisionReportDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCreateSingleDecisionReportRequest(singleDecisionReportDefinitionDto)
            .execute();

    // then
    assertThat(createResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void combinedProcessReportCanNotBeCreatedForInvalidCollection() {
    // given
    CombinedReportDefinitionRequestDto combinedReportDefinitionDto =
        new CombinedReportDefinitionRequestDto();
    combinedReportDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCreateCombinedReportRequest(combinedReportDefinitionDto)
            .execute();

    // then
    assertThat(createResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void dashboardCanNotBeCreatedForInvalidCollection() {
    // given
    DashboardDefinitionRestDto dashboardDefinitionDto = new DashboardDefinitionRestDto();
    dashboardDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCreateDashboardRequest(dashboardDefinitionDto)
            .execute();

    // then
    assertThat(createResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void collectionItemsAreOrderedByTypeAndModificationDateDescending() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String reportId1 = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    String reportId2 = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    String dashboardId1 = dashboardClient.createEmptyDashboard(collectionId);
    String dashboardId2 = dashboardClient.createEmptyDashboard(collectionId);

    reportClient.updateSingleProcessReport(
        reportId1, new SingleProcessReportDefinitionRequestDto());

    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    // then
    assertThat(collectionEntities.get(0).getId()).isEqualTo(dashboardId2);
    assertThat(collectionEntities.get(1).getId()).isEqualTo(dashboardId1);
    assertThat(collectionEntities.get(2).getId()).isEqualTo(reportId1);
    assertThat(collectionEntities.get(3).getId()).isEqualTo(reportId2);
  }

  @Test
  public void doNotUpdateNullFieldsInCollection() {
    // given
    String id = collectionClient.createNewCollection();
    PartialCollectionDefinitionRequestDto collection = new PartialCollectionDefinitionRequestDto();

    // when
    collectionClient.updateCollection(id, collection);
    CollectionDefinitionRestDto storedCollection = collectionClient.getCollectionById(id);

    // then
    assertThat(storedCollection.getId()).isEqualTo(id);
    assertThat(storedCollection.getCreated()).isNotNull();
    assertThat(storedCollection.getLastModified()).isNotNull();
    assertThat(storedCollection.getLastModifier()).isNotNull();
    assertThat(storedCollection.getName()).isNotNull();
    assertThat(storedCollection.getOwner()).isNotNull();
  }

  @Test
  public void deletedReportsAreRemovedFromCollectionWhenForced() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String singleReportIdToDelete =
        reportClient.createEmptySingleProcessReportInCollection(collectionId);
    String combinedReportIdToDelete = reportClient.createEmptyCombinedReport(collectionId);

    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    reportClient.deleteReport(singleReportIdToDelete, true);
    reportClient.deleteReport(combinedReportIdToDelete, true);

    // then
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);
    assertThat(collectionEntities).isEmpty();
  }

  @Test
  public void deletedDashboardsAreRemovedFromCollectionWhenForced() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String dashboardIdToDelete = dashboardClient.createEmptyDashboard(collectionId);

    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    dashboardClient.deleteDashboard(dashboardIdToDelete, true);

    // then
    List<EntityResponseDto> collectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);
    assertThat(collectionEntities).isEmpty();
  }

  @Test
  public void entitiesAreDeletedOnCollectionDelete() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String singleReportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    String combinedReportId = reportClient.createEmptyCombinedReport(collectionId);
    String dashboardId = dashboardClient.createEmptyDashboard(collectionId);

    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    collectionClient.deleteCollection(collectionId);

    // then
    collectionClient.assertCollectionIsDeleted(collectionId);

    dashboardClient.assertDashboardIsDeleted(dashboardId);
    reportClient.assertReportIsDeleted(singleReportId);
    reportClient.assertReportIsDeleted(combinedReportId);
  }

  @Test
  public void collectionNotDeletedIfDbFailsWhenDeletingContainedAlerts() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String reportId1 = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    final String reportId2 = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    alertClient.createAlertForReport(reportId1);
    alertClient.createAlertForReport(reportId1);
    alertClient.createAlertForReport(reportId2);

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request().withPath("/.*-" + ALERT_INDEX_NAME + "/_delete_by_query").withMethod(POST);
    dbMockServer.when(requestMatcher, Times.once()).error(error().withDropConnection(true));

    // when
    collectionClient.deleteCollection(collectionId);

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    Integer alertCount = databaseIntegrationTestExtension.getDocumentCountOf(ALERT_INDEX_NAME);
    assertThat(alertCount).isEqualTo(3);
    assertThat(collectionClient.getCollectionById(collectionId)).isNotNull();
  }

  @Test
  public void collectionNotDeletedIfDbFailsWhenDeletingContainedReport() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String reportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request()
            .withPath("/.*" + SINGLE_PROCESS_REPORT_INDEX_NAME + ".*/_delete_by_query")
            .withMethod(POST);
    dbMockServer.when(requestMatcher, Times.once()).error(error().withDropConnection(true));

    // when
    collectionClient.deleteCollection(collectionId);

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    assertThat(reportClient.getSingleProcessReportDefinitionDto(reportId)).isNotNull();
    assertThat(collectionClient.getCollectionById(collectionId)).isNotNull();
  }

  @Test
  public void collectionNotDeletedIfDbFailsWhenDeletingContainedDashboard() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String dashboardId = dashboardClient.createEmptyDashboard(collectionId);

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request().withPath("/.*-" + DASHBOARD_INDEX_NAME + "/_delete_by_query").withMethod(POST);
    dbMockServer.when(requestMatcher, Times.once()).error(error().withDropConnection(true));

    // when
    collectionClient.deleteCollection(collectionId);

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    assertThat(dashboardClient.getDashboard(dashboardId)).isNotNull();
    assertThat(collectionClient.getCollectionById(collectionId)).isNotNull();
  }

  @Test
  public void copyAnEmptyCollectionWithCustomPermissionsAndScope() {
    // given
    engineIntegrationExtension.addUser("kermit", "kermit");
    engineIntegrationExtension.grantUserOptimizeAccess("kermit");
    String collectionId = collectionClient.createNewCollection();
    collectionClient.addRolesToCollection(
        collectionId,
        new CollectionRoleRequestDto(
            new IdentityDto("kermit", IdentityType.USER), RoleType.EDITOR));

    collectionClient.addScopeEntryToCollection(
        collectionId, new CollectionScopeEntryDto("PROCESS:invoice"));

    // when
    String copyId = collectionClient.copyCollection(collectionId).getId();
    CollectionDefinitionRestDto copyDefinition = collectionClient.getCollectionById(copyId);

    // then
    assertThat(copyDefinition.getName().toLowerCase(Locale.ENGLISH).contains("copy"))
        .isEqualTo(true);
  }

  @Test
  public void copyCollectionWithASingleReport() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String originalReportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);

    // when
    String copyId = collectionClient.copyCollection(collectionId).getId();

    List<EntityResponseDto> copiedCollectionEntities =
        collectionClient.getEntitiesForCollection(copyId);

    String reportCopyId = copiedCollectionEntities.get(0).getId();

    SingleProcessReportDefinitionRequestDto originalReport =
        reportClient.getSingleProcessReportDefinitionDto(originalReportId);

    SingleProcessReportDefinitionRequestDto copiedReport =
        reportClient.getSingleProcessReportDefinitionDto(reportCopyId);

    // then
    assertThat(originalReport.getData()).isEqualTo(copiedReport.getData());
    assertThat(copiedReport.getName()).isEqualTo(originalReport.getName());
  }

  @Test
  public void copyCollectionWithADashboard() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String originalReportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    String dashboardId = dashboardClient.createEmptyDashboard(collectionId);

    DashboardDefinitionRestDto dashboardDefinition = dashboardClient.getDashboard(dashboardId);

    dashboardDefinition.setTiles(
        Collections.singletonList(
            new DashboardReportTileDto(
                originalReportId,
                new PositionDto(),
                new DimensionDto(),
                DashboardTileType.OPTIMIZE_REPORT,
                null)));

    dashboardClient.updateDashboard(dashboardId, dashboardDefinition);

    // when
    String copyId = collectionClient.copyCollection(collectionId).getId();

    collectionClient.getCollectionById(copyId);
    List<EntityResponseDto> copiedCollectionEntities =
        collectionClient.getEntitiesForCollection(collectionId);

    String copiedDashboardId =
        copiedCollectionEntities.stream()
            .filter(e -> e.getEntityType().equals(EntityType.DASHBOARD))
            .findFirst()
            .get()
            .getId();

    String copiedReportId =
        copiedCollectionEntities.stream()
            .filter(e -> e.getEntityType().equals(EntityType.REPORT))
            .findFirst()
            .get()
            .getId();

    DashboardDefinitionRestDto copiedDashboard = dashboardClient.getDashboard(copiedDashboardId);

    SingleProcessReportDefinitionRequestDto copiedReportDefinition =
        reportClient.getSingleProcessReportDefinitionDto(copiedReportId);
    SingleProcessReportDefinitionRequestDto originalReportDefinition =
        reportClient.getSingleProcessReportDefinitionDto(originalReportId);
    // then
    // the dashboard references the same report entity as the report itself
    assertThat(copiedDashboard.getTiles().get(0).getId()).isEqualTo(copiedReportId);

    assertThat(copiedDashboard.getName()).isEqualTo(dashboardDefinition.getName());
    assertThat(copiedReportDefinition.getName()).isEqualTo(originalReportDefinition.getName());

    assertThat(copiedCollectionEntities).hasSize(2);
    assertThat(copiedCollectionEntities.stream().anyMatch(e -> e.getId().equals(copiedReportId)))
        .isEqualTo(true);
    assertThat(copiedCollectionEntities.stream().anyMatch(e -> e.getId().equals(copiedDashboardId)))
        .isEqualTo(true);
  }

  @Test
  public void copyCollectionWithANestedReport() {
    // given
    String collectionId = collectionClient.createNewCollection();
    String originalReportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    String combinedReportId = reportClient.createEmptyCombinedReport(collectionId);
    String dashboardId = dashboardClient.createEmptyDashboard(collectionId);

    DashboardDefinitionRestDto dashboardDefinition = dashboardClient.getDashboard(dashboardId);
    CombinedReportDefinitionRequestDto combinedReportDefinition =
        reportClient.getCombinedProcessReportById(combinedReportId);

    reportClient.updateCombinedReport(combinedReportId, Lists.newArrayList(originalReportId));

    ArrayList<DashboardReportTileDto> dashboardTileDtos = new ArrayList<>();
    dashboardTileDtos.add(
        new DashboardReportTileDto(
            combinedReportId,
            new PositionDto(),
            new DimensionDto(),
            DashboardTileType.OPTIMIZE_REPORT,
            null));
    dashboardTileDtos.add(
        new DashboardReportTileDto(
            originalReportId,
            new PositionDto(),
            new DimensionDto(),
            DashboardTileType.OPTIMIZE_REPORT,
            null));

    dashboardDefinition.setTiles(dashboardTileDtos);

    dashboardClient.updateDashboard(dashboardId, dashboardDefinition);

    String copiedCollectionId = collectionClient.copyCollection(collectionId).getId();
    List<EntityResponseDto> copiedCollectionEntities =
        collectionClient.getEntitiesForCollection(copiedCollectionId);
    SingleProcessReportDefinitionRequestDto originalSingleReportDefinition =
        reportClient.getSingleProcessReportDefinitionDto(originalReportId);

    assertThat(copiedCollectionEntities).hasSize(3);

    List<String> copiedCollectionEntityNames =
        copiedCollectionEntities.stream()
            .map(EntityResponseDto::getName)
            .collect(Collectors.toList());

    assertThat(copiedCollectionEntityNames)
        .containsExactlyInAnyOrder(
            dashboardDefinition.getName(),
            combinedReportDefinition.getName(),
            originalSingleReportDefinition.getName());
  }

  @Test
  public void copyCollectionWithNewName() {
    String collectionId = collectionClient.createNewCollection();

    String copyWithoutNewNameId = collectionClient.copyCollection(collectionId).getId();
    CollectionDefinitionRestDto copiedCollectionWithoutNewName =
        collectionClient.getCollectionById(copyWithoutNewNameId);

    String copyWithNewNameId = collectionClient.copyCollection(collectionId, "newCoolName").getId();
    CollectionDefinitionRestDto copiedCollectionWithNewName =
        collectionClient.getCollectionById(copyWithNewNameId);

    CollectionDefinitionRestDto originalCollection =
        collectionClient.getCollectionById(collectionId);

    assertThat(copiedCollectionWithNewName.getName()).isEqualTo("newCoolName");
    assertThat(copiedCollectionWithoutNewName.getName().contains(originalCollection.getName()))
        .isEqualTo(true);
    assertThat(
            copiedCollectionWithoutNewName.getName().toLowerCase(Locale.ENGLISH).contains("copy"))
        .isEqualTo(true);
  }

  @ParameterizedTest(
      name = "Copy collection and all alerts within the collection for report definition type {0}")
  @MethodSource("definitionTypes")
  public void copyCollectionAndAllContainingAlerts(final DefinitionType definitionType) {
    // given
    String originalId = collectionClient.createNewCollectionWithDefaultScope(definitionType);

    List<String> reportsToCopy = new ArrayList<>();
    reportsToCopy.add(
        reportClient.createReportForCollectionAsUser(
            originalId, definitionType, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS));
    reportsToCopy.add(
        reportClient.createReportForCollectionAsUser(
            originalId, definitionType, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS));

    List<String> alertsToCopy = new ArrayList<>();
    reportsToCopy.forEach(reportId -> alertsToCopy.add(alertClient.createAlertForReport(reportId)));

    // when
    CollectionDefinitionRestDto copy = collectionClient.copyCollection(originalId);

    // then
    List<AuthorizedReportDefinitionResponseDto> copiedReports =
        collectionClient.getReportsForCollection(copy.getId());
    List<AlertDefinitionDto> copiedAlerts = collectionClient.getAlertsForCollection(copy.getId());
    Set<String> copiedReportIdsWithAlert =
        copiedAlerts.stream().map(AlertCreationRequestDto::getReportId).collect(toSet());

    assertThat(copiedReports).hasSize(reportsToCopy.size());
    assertThat(copiedAlerts).hasSize(alertsToCopy.size());
    assertThat(copiedReportIdsWithAlert).hasSize(copiedReports.size());
    assertThat(
            copiedReports.stream()
                .allMatch(
                    report -> copiedReportIdsWithAlert.contains(report.getDefinitionDto().getId())))
        .isTrue();
  }

  @Test
  public void copyCollectionWithAReport_entitiesNotCopiedIfCollectionCreationFailsOnDbFailure() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String reportId = reportClient.createEmptySingleProcessReportInCollection(collectionId);
    final String alertId = alertClient.createAlertForReport(reportId);
    final String dashboardId =
        dashboardClient.createDashboard(collectionId, singletonList(reportId));

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request().withPath("/.*-" + COLLECTION_INDEX_NAME + "/_doc/.*").withMethod(PUT);
    dbMockServer.when(requestMatcher, Times.once()).error(error().withDropConnection(true));

    // when
    embeddedOptimizeExtension
        .getRequestExecutor()
        .buildCopyCollectionRequest(collectionId)
        .execute(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    // then only original entities exist
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    assertThat(getAllStoredCollections())
        .extracting(CollectionDefinitionDto::getId)
        .containsExactly(collectionId);
    assertThat(getAllStoredProcessReports())
        .extracting(SingleProcessReportDefinitionRequestDto::getId)
        .containsExactly(reportId);
    assertThat(getAllStoredDashboards())
        .extracting(DashboardDefinitionRestDto::getId)
        .containsExactly(dashboardId);
    assertThat(getAllStoredAlerts()).extracting(AlertDefinitionDto::getId).containsExactly(alertId);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void deleteSingleScopeOverrulesConflictsOnForceSet(final DefinitionType definitionType) {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final CollectionScopeEntryDto scopeEntry =
        new CollectionScopeEntryDto(definitionType, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS);
    collectionClient.addScopeEntryToCollection(collectionId, scopeEntry);
    final String reportId =
        reportClient.createSingleReport(
            collectionId, definitionType, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS);
    alertClient.createAlertForReport(reportId);
    final String dashboardId =
        dashboardClient.createDashboard(collectionId, singletonList(reportId));

    // when
    collectionClient.deleteScopeEntry(collectionId, scopeEntry, true);

    // then
    assertThat(dashboardClient.getDashboard(dashboardId).getTiles()).isEmpty();
    assertThat(collectionClient.getReportsForCollection(collectionId)).isEmpty();
    assertThat(collectionClient.getAlertsForCollection(collectionId)).isEmpty();
    assertThat(collectionClient.getCollectionScope(collectionId)).isEmpty();
  }

  @Test
  public void deleteSingleScopeOverrulesConflictsOnForceSetForMultiDefinitionProcessReport() {
    // given
    final String collectionId = collectionClient.createNewCollection();

    final String definitionKey1 = DEFAULT_DEFINITION_KEY;
    engineIntegrationExtension.deployProcessAndGetId(
        BpmnModels.getSimpleBpmnDiagram(definitionKey1));
    final CollectionScopeEntryDto scopeEntry1 =
        new CollectionScopeEntryDto(PROCESS, definitionKey1, DEFAULT_TENANTS);
    collectionClient.addScopeEntryToCollection(collectionId, scopeEntry1);

    final String definitionKey2 = "key2";
    engineIntegrationExtension.deployProcessAndGetId(
        BpmnModels.getSimpleBpmnDiagram(definitionKey2));
    final CollectionScopeEntryDto scopeEntry2 =
        new CollectionScopeEntryDto(PROCESS, definitionKey2, DEFAULT_TENANTS);
    collectionClient.addScopeEntryToCollection(collectionId, scopeEntry2);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.RAW_DATA)
            .definitions(
                List.of(
                    new ReportDataDefinitionDto(definitionKey1),
                    new ReportDataDefinitionDto(definitionKey2)))
            .build();
    final String reportId = reportClient.createSingleProcessReport(reportData, collectionId);
    alertClient.createAlertForReport(reportId);
    final String dashboardId =
        dashboardClient.createDashboard(collectionId, singletonList(reportId));

    // when
    collectionClient.deleteScopeEntry(collectionId, scopeEntry2, true);

    // then
    assertThat(dashboardClient.getDashboard(dashboardId).getTiles()).isEmpty();
    assertThat(collectionClient.getReportsForCollection(collectionId)).isEmpty();
    assertThat(collectionClient.getAlertsForCollection(collectionId)).isEmpty();
    assertThat(collectionClient.getCollectionScope(collectionId))
        .extracting(CollectionScopeEntryResponseDto::getDefinitionKey)
        .containsExactly(definitionKey1);
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void deleteSingleScopeOverrulesCombinedReportConflictsOnForceSet() {
    // given
    String collectionId = collectionClient.createNewCollection();
    final CollectionScopeEntryDto scopeEntry =
        new CollectionScopeEntryDto(PROCESS, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS);
    collectionClient.addScopeEntryToCollection(collectionId, scopeEntry);
    final String singleReportId =
        reportClient.createSingleReport(
            collectionId, PROCESS, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS);
    final String combinedReportId =
        reportClient.createCombinedReport(collectionId, singletonList(singleReportId));

    // when
    collectionClient.deleteScopeEntry(collectionId, scopeEntry, true);

    // then
    assertThat(collectionClient.getReportsForCollection(collectionId))
        .hasSize(1)
        .extracting(AuthorizedReportDefinitionResponseDto::getDefinitionDto)
        .extracting(r -> (CombinedReportDefinitionRequestDto) r)
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(combinedReportId);
              assertThat(r.getData().getReportIds()).isEmpty();
            });
    assertThat(collectionClient.getCollectionScope(collectionId)).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void updateSingleScope_oneTenantRemoved(DefinitionType definitionType) {
    // given
    String collectionId = collectionClient.createNewCollection();
    addTenantToElasticsearch("tenantToBeRemovedFromScope");
    final ArrayList<String> tenants = new ArrayList<>(DEFAULT_TENANTS);
    tenants.add("tenantToBeRemovedFromScope");
    final CollectionScopeEntryDto scopeEntry =
        new CollectionScopeEntryDto(definitionType, DEFAULT_DEFINITION_KEY, tenants);
    collectionClient.addScopeEntryToCollection(collectionId, scopeEntry);
    reportClient.createSingleReport(collectionId, definitionType, DEFAULT_DEFINITION_KEY, tenants);

    // when
    tenants.remove("tenantToBeRemovedFromScope");
    collectionClient.updateCollectionScopeEntry(
        collectionId, new CollectionScopeEntryUpdateDto(tenants), scopeEntry.getId());

    // then
    assertThat(collectionClient.getReportsForCollection(collectionId))
        .hasSize(1)
        .extracting(AuthorizedReportDefinitionResponseDto::getDefinitionDto)
        .extracting(r -> (SingleReportDefinitionDto<?>) r)
        .extracting(SingleReportDefinitionDto::getData)
        .flatExtracting(SingleReportDataDto::getTenantIds)
        .containsExactlyElementsOf(tenants);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void updateSingleScope_reportsWithoutTenantsAreNotBeingRemoved(
      DefinitionType definitionType) {
    // given
    String collectionId = collectionClient.createNewCollection();
    addTenantToElasticsearch("tenantToBeRemovedFromScope");
    final ArrayList<String> tenants = new ArrayList<>(DEFAULT_TENANTS);
    tenants.add("tenantToBeRemovedFromScope");
    final CollectionScopeEntryDto scopeEntry =
        new CollectionScopeEntryDto(definitionType, DEFAULT_DEFINITION_KEY, tenants);
    collectionClient.addScopeEntryToCollection(collectionId, scopeEntry);
    final String singleReportId =
        reportClient.createSingleReport(
            collectionId,
            definitionType,
            DEFAULT_DEFINITION_KEY,
            singletonList("tenantToBeRemovedFromScope"));
    final String alertId = alertClient.createAlertForReport(singleReportId);
    final String dashboardId =
        dashboardClient.createDashboard(collectionId, singletonList(singleReportId));

    // when
    tenants.remove("tenantToBeRemovedFromScope");
    collectionClient.updateCollectionScopeEntry(
        collectionId, new CollectionScopeEntryUpdateDto(tenants), scopeEntry.getId());

    // then
    assertThat(dashboardClient.getDashboard(dashboardId).getTiles())
        .extracting(DashboardReportTileDto::getId)
        .contains(singleReportId);
    assertThat(collectionClient.getReportsForCollection(collectionId))
        .extracting(AuthorizedReportDefinitionResponseDto::getDefinitionDto)
        .extracting(ReportDefinitionDto::getId)
        .contains(singleReportId);
    assertThat(collectionClient.getAlertsForCollection(collectionId))
        .extracting(AlertDefinitionDto::getId)
        .contains(alertId);
  }

  private List<SingleProcessReportDefinitionRequestDto> getAllStoredProcessReports() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        SINGLE_PROCESS_REPORT_INDEX_NAME, SingleProcessReportDefinitionRequestDto.class);
  }

  private List<CollectionDefinitionDto> getAllStoredCollections() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        COLLECTION_INDEX_NAME, CollectionDefinitionDto.class);
  }

  private List<AlertDefinitionDto> getAllStoredAlerts() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        ALERT_INDEX_NAME, AlertDefinitionDto.class);
  }

  private List<DashboardDefinitionRestDto> getAllStoredDashboards() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        DASHBOARD_INDEX_NAME, DashboardDefinitionRestDto.class);
  }

  private void addTenantToElasticsearch(final String tenantId) {
    TenantDto tenantDto = new TenantDto(tenantId, "ATenantName", DEFAULT_ENGINE_ALIAS);
    databaseIntegrationTestExtension.addEntryToDatabase(TENANT_INDEX_NAME, tenantId, tenantDto);
  }
}
