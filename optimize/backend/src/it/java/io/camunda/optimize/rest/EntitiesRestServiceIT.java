/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.rest;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto.Fields.name;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static io.camunda.optimize.rest.RestTestUtil.getOffsetDiffInHours;
// import static io.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_TIMEZONE;
// import static io.camunda.optimize.service.db.DatabaseConstants.COLLECTION_INDEX_NAME;
// import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_INDEX_NAME;
// import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
// import static io.camunda.optimize.service.db.writer.CollectionWriter.DEFAULT_COLLECTION_NAME;
// import static
// io.camunda.optimize.service.util.ProcessReportDataBuilderHelper.createCombinedReportData;
// import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
// import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
// import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
// import static jakarta.ws.rs.HttpMethod.DELETE;
// import static jakarta.ws.rs.HttpMethod.POST;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockserver.model.HttpRequest.request;
//
// import com.google.common.collect.ImmutableMap;
// import com.google.common.collect.Lists;
// import io.camunda.optimize.dto.optimize.IdentityDto;
// import io.camunda.optimize.dto.optimize.IdentityType;
// import io.camunda.optimize.dto.optimize.ReportType;
// import io.camunda.optimize.dto.optimize.RoleType;
// import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleRequestDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto;
// import io.camunda.optimize.dto.optimize.query.entity.EntitiesDeleteRequestDto;
// import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
// import io.camunda.optimize.dto.optimize.query.entity.EntityType;
// import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionRequestDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
// import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
// import io.camunda.optimize.dto.optimize.rest.sorting.EntitySorter;
// import io.camunda.optimize.service.dashboard.InstantPreviewDashboardService;
// import io.camunda.optimize.service.util.configuration.users.AuthorizedUserType;
// import jakarta.ws.rs.core.Response;
// import java.time.OffsetDateTime;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.List;
// import java.util.Optional;
// import java.util.stream.Stream;
// import org.assertj.core.groups.Tuple;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
// import org.junit.jupiter.params.provider.ValueSource;
// import org.mockserver.integration.ClientAndServer;
// import org.mockserver.matchers.Times;
// import org.mockserver.model.HttpError;
// import org.mockserver.model.HttpRequest;
// import org.mockserver.verify.VerificationTimes;
//
// @Tag(OPENSEARCH_PASSING)
// public class EntitiesRestServiceIT extends AbstractEntitiesRestServiceIT {
//
//   @Test
//   public void getEntities_WithoutAuthentication() {
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .withoutAuthentication()
//             .buildGetAllPrivateReportsRequest()
//             .execute();
//
//     // then the status code is not authorized
//     assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
//   }
//
//   @ValueSource(booleans = {true, false})
//   @ParameterizedTest
//   public void getEntities_returnsMyUsersReports(final boolean isSuperUser) {
//     // given
//     if (isSuperUser) {
//       embeddedOptimizeExtension
//           .getConfigurationService()
//           .getAuthConfiguration()
//           .getSuperUserIds()
//           .add(DEFAULT_USERNAME);
//     }
//     embeddedOptimizeExtension.getManagementDashboardService().init();
//     addSingleReportToOptimize("B Report", ReportType.PROCESS);
//     addSingleReportToOptimize("A Report", ReportType.DECISION);
//     addCombinedReport("D Combined");
//     // we also initialise an instant dashboard
//     final InstantPreviewDashboardService instantPreviewDashboardService =
//         embeddedOptimizeExtension.getInstantPreviewDashboardService();
//     String processDefKey = "dummy";
//     String dashboardJsonTemplateFilename = "template2.json";
//     engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(processDefKey));
//     importAllEngineEntitiesFromScratch();
//     final Optional<InstantDashboardDataDto> instantPreviewDashboard =
//         instantPreviewDashboardService.createInstantPreviewDashboard(
//             processDefKey, dashboardJsonTemplateFilename);
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> privateEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(privateEntities)
//         .hasSize(3)
//         .extracting(EntityResponseDto::getReportType, EntityResponseDto::getCombined)
//         // The created management reports and instant preview dashboard reports are excluded from
//         // the results
//         .containsExactlyInAnyOrder(
//             Tuple.tuple(ReportType.PROCESS, true),
//             Tuple.tuple(ReportType.PROCESS, false),
//             Tuple.tuple(ReportType.DECISION, false));
//   }
//
//   @Test
//   public void getEntities_entitiesHaveDescriptions() {
//     // given
//     addSingleReportWithDescriptionToOptimize("B Report", ReportType.PROCESS, "a process report");
//     addSingleReportWithDescriptionToOptimize("A Report", ReportType.DECISION, "a decision
// report");
//     addCollection("some collection");
//     addDashboardWithDescriptionToOptimize("Empty dashboard", "a dashboard");
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final List<EntityResponseDto> privateEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(privateEntities)
//         .hasSize(4)
//         .extracting(EntityResponseDto::getName, EntityResponseDto::getDescription)
//         .containsExactlyInAnyOrder(
//             Tuple.tuple("B Report", "a process report"),
//             Tuple.tuple("A Report", "a decision report"),
//             Tuple.tuple("some collection", null),
//             Tuple.tuple("Empty dashboard", "a dashboard"));
//   }
//
//   @Test
//   public void getEntities_adoptTimezoneFromHeader() {
//     // given
//     OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();
//
//     addSingleReportToOptimize("My Report", ReportType.PROCESS);
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> privateEntities =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetAllEntitiesRequest()
//             .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
//             .executeAndReturnList(EntityResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(privateEntities).isNotNull().hasSize(1);
//     EntityResponseDto entityDto = privateEntities.get(0);
//     assertThat(entityDto.getCreated()).isEqualTo(now);
//     assertThat(entityDto.getLastModified()).isEqualTo(now);
//     assertThat(getOffsetDiffInHours(entityDto.getCreated(), now)).isEqualTo(1.);
//     assertThat(getOffsetDiffInHours(entityDto.getLastModified(), now)).isEqualTo(1.);
//   }
//
//   @Test
//   public void getEntities_DoesNotReturnOtherUsersReports() {
//     // given
//     engineIntegrationExtension.addUser("kermit", "kermit");
//     engineIntegrationExtension.grantUserOptimizeAccess("kermit");
//     addSingleReportToOptimize("B Report", ReportType.PROCESS, null, "kermit");
//     addSingleReportToOptimize("A Report", ReportType.DECISION);
//     addCombinedReport("D Combined");
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when (default user)
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(2)
//         .extracting(EntityResponseDto::getName)
//         .containsExactlyInAnyOrder("A Report", "D Combined");
//
//     // when
//     final List<EntityResponseDto> kermitUserEntities =
//         entitiesClient.getAllEntitiesAsUser(KERMIT_USER, KERMIT_USER);
//
//     // then
//     assertThat(kermitUserEntities)
//         .hasSize(1)
//         .extracting(EntityResponseDto::getName)
//         .containsExactly("B Report");
//   }
//
//   @Test
//   public void getEntities_emptyDefinitionKeyIsHandledAsEmptyReport() {
//     // this is a regression test that could occur for old empty reports
//     // see https://jira.camunda.com/browse/OPT-3496
//
//     // given
//     SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
//         new SingleProcessReportDefinitionRequestDto();
//     singleProcessReportDefinitionDto.setName("empty");
//     // an empty string definition key caused trouble
//     singleProcessReportDefinitionDto.getData().setProcessDefinitionKey("");
//     reportClient.createSingleProcessReport(singleProcessReportDefinitionDto);
//
//     // when (default user)
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(1)
//         .extracting(EntityResponseDto::getName)
//         .containsExactly(singleProcessReportDefinitionDto.getName());
//   }
//
//   @Test
//   public void getEntities_ReturnsMyUsersDashboards() {
//     // given
//     addDashboardToOptimize("A Dashboard");
//     addDashboardToOptimize("B Dashboard");
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> privateEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(privateEntities).hasSize(2);
//   }
//
//   @Test
//   public void getEntities_DoesNotReturnOtherUsersDashboards() {
//     // given
//     engineIntegrationExtension.addUser("kermit", "kermit");
//     engineIntegrationExtension.grantUserOptimizeAccess("kermit");
//     addDashboardToOptimize("A Dashboard");
//     addDashboardToOptimize("B Dashboard", null, "kermit");
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when (default user)
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(1)
//         .extracting(EntityResponseDto::getName)
//         .containsExactly("A Dashboard");
//
//     // when
//     final List<EntityResponseDto> kermitUserEntities =
//         entitiesClient.getAllEntitiesAsUser(KERMIT_USER, KERMIT_USER);
//
//     // then
//     assertThat(kermitUserEntities)
//         .hasSize(1)
//         .extracting(EntityResponseDto::getName)
//         .containsExactly("B Dashboard");
//   }
//
//   @Test
//   public void getEntities_ReturnsCollections() {
//     // given
//     collectionClient.createNewCollection();
//     collectionClient.createNewCollection();
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> privateEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(privateEntities).hasSize(2);
//   }
//
//   @Test
//   public void getEntities_DoesNotReturnEntitiesInCollections() {
//     // given
//     final String collectionId = collectionClient.createNewCollection();
//
//     addSingleReportToOptimize("A Report", ReportType.DECISION);
//     addSingleReportToOptimize("B Report", ReportType.PROCESS, collectionId, DEFAULT_USERNAME);
//     addDashboardToOptimize("C Dashboard", collectionId, DEFAULT_USERNAME);
//     addCombinedReport("D Combined");
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(3)
//         .extracting(EntityResponseDto::getName)
//         .containsExactlyInAnyOrder("A Report", "D Combined", DEFAULT_COLLECTION_NAME);
//   }
//
//   @Test
//   public void getEntities__noSortApplied_OrderedByTypeAndLastModified() {
//     // given
//     addCollection("B Collection");
//     addCollection("A Collection");
//     addSingleReportToOptimize("D Report", ReportType.PROCESS);
//     addSingleReportToOptimize("C Report", ReportType.DECISION);
//     addDashboardToOptimize("B Dashboard");
//     addDashboardToOptimize("A Dashboard");
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> entities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(entities)
//         .hasSize(6)
//         .extracting(EntityResponseDto::getName, EntityResponseDto::getEntityType)
//         .containsExactly(
//             Tuple.tuple("A Collection", EntityType.COLLECTION),
//             Tuple.tuple("B Collection", EntityType.COLLECTION),
//             Tuple.tuple("A Dashboard", EntityType.DASHBOARD),
//             Tuple.tuple("B Dashboard", EntityType.DASHBOARD),
//             Tuple.tuple("C Report", EntityType.REPORT),
//             Tuple.tuple("D Report", EntityType.REPORT));
//   }
//
//   @Test
//   public void getEntities_viewerRoleTypeIsAppliedIfUserHasNoEditorAuthorization() {
//     // given
//     addCollection("B Collection");
//     addCollection("A Collection");
//     addSingleReportToOptimize("D Report", ReportType.PROCESS);
//     addSingleReportToOptimize("C Report", ReportType.DECISION);
//     addDashboardToOptimize("B Dashboard");
//     addDashboardToOptimize("A Dashboard");
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getEntityConfiguration()
//         .setAuthorizedUserType(AuthorizedUserType.NONE);
//     embeddedOptimizeExtension.reloadConfiguration();
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> entities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(entities)
//         .hasSize(6)
//         .extracting(EntityResponseDto::getEntityType, EntityResponseDto::getCurrentUserRole)
//         .containsExactlyInAnyOrder(
//             // For collections, the user is the creator and therefore still has manager role
//             Tuple.tuple(EntityType.COLLECTION, RoleType.MANAGER),
//             Tuple.tuple(EntityType.COLLECTION, RoleType.MANAGER),
//             // Otherwise, the user has viewer role as they have no entity editor authorization
//             Tuple.tuple(EntityType.DASHBOARD, RoleType.VIEWER),
//             Tuple.tuple(EntityType.DASHBOARD, RoleType.VIEWER),
//             Tuple.tuple(EntityType.REPORT, RoleType.VIEWER),
//             Tuple.tuple(EntityType.REPORT, RoleType.VIEWER));
//   }
//
//   @Test
//   @Tag(OPENSEARCH_SHOULD_BE_PASSING)
//   // Probable cause is implementation error in EntitiesReaderOS.countEntitiesForCollections
//   public void getEntities_IncludesCollectionSubEntityCountsIfThereAreNoEntities() {
//     // given
//     collectionClient.createNewCollection();
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(1)
//         .allSatisfy(
//             entry ->
//                 assertThat(entry.getData().getSubEntityCounts())
//                     .hasSize(2)
//                     .containsExactlyInAnyOrderEntriesOf(
//                         ImmutableMap.of(
//                             EntityType.REPORT, 0L,
//                             EntityType.DASHBOARD, 0L)));
//   }
//
//   @Test
//   @Tag(OPENSEARCH_SHOULD_BE_PASSING)
//   // Probable cause is implementation error in EntitiesReaderOS.countEntitiesForCollections
//   public void getEntities_IncludesCollectionSubEntityCounts() {
//     // given
//     final String collectionId = collectionClient.createNewCollection();
//
//     addSingleReportToOptimize("A Report", ReportType.DECISION, collectionId, DEFAULT_USERNAME);
//     addSingleReportToOptimize("B Report", ReportType.PROCESS, collectionId, DEFAULT_USERNAME);
//     addDashboardToOptimize("C Dashboard", collectionId, DEFAULT_USERNAME);
//     addCombinedReport("D Combined", collectionId);
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(1)
//         .allSatisfy(
//             entry ->
//                 assertThat(entry.getData().getSubEntityCounts())
//                     .hasSize(2)
//                     .containsExactlyInAnyOrderEntriesOf(
//                         ImmutableMap.of(
//                             EntityType.REPORT, 3L,
//                             EntityType.DASHBOARD, 1L)));
//   }
//
//   @Test
//   public void getEntities_IncludesCollectionRoleCountsByDefault() {
//     // given
//     collectionClient.createNewCollection();
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(1)
//         .allSatisfy(
//             entry ->
//                 assertThat(entry.getData().getRoleCounts())
//                     .hasSize(2)
//                     .containsExactlyInAnyOrderEntriesOf(
//                         ImmutableMap.of(
//                             IdentityType.USER, 1L,
//                             IdentityType.GROUP, 0L)));
//   }
//
//   @Test
//   public void getEntities_IncludesCollectionRoleCounts() {
//     // given
//     final String collectionId = collectionClient.createNewCollection();
//     final String user1 = "user1";
//     authorizationClient.addUserAndGrantOptimizeAccess(user1);
//     addRoleToCollection(collectionId, user1, IdentityType.USER);
//     final String groupA = "groupA";
//     authorizationClient.createGroupAndGrantOptimizeAccess(groupA, groupA);
//     addRoleToCollection(collectionId, groupA, IdentityType.GROUP);
//     final String groupB = "groupB";
//     authorizationClient.createGroupAndGrantOptimizeAccess(groupB, groupB);
//     addRoleToCollection(collectionId, groupB, IdentityType.GROUP);
//     final String groupC = "groupC";
//     authorizationClient.createGroupAndGrantOptimizeAccess(groupC, groupC);
//     addRoleToCollection(collectionId, groupC, IdentityType.GROUP);
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(1)
//         .allSatisfy(
//             entry ->
//                 assertThat(entry.getData().getRoleCounts())
//                     .hasSize(2)
//                     .containsExactlyInAnyOrderEntriesOf(
//                         ImmutableMap.of(
//                             IdentityType.USER, 2L,
//                             IdentityType.GROUP, 3L)));
//   }
//
//   @Test
//   public void getEntities_IncludesPrivateCombinedReportSubEntityCounts() {
//     // given
//     final String reportId1 = addSingleReportToOptimize("A Report", ReportType.PROCESS);
//     final String reportId2 = addSingleReportToOptimize("B Report", ReportType.PROCESS);
//     final String combinedReportId = addCombinedReport("D Combined");
//
//     final CombinedReportDefinitionRequestDto combinedReportUpdate =
//         new CombinedReportDefinitionRequestDto();
//     combinedReportUpdate.setData(createCombinedReportData(reportId1, reportId2));
//     reportClient.updateCombinedReport(combinedReportId, Lists.newArrayList(reportId1,
// reportId2));
//
//     // when
//     final List<EntityResponseDto> defaultUserEntities = entitiesClient.getAllEntities();
//
//     // then
//     assertThat(defaultUserEntities)
//         .hasSize(3)
//         .filteredOn(EntityResponseDto::getCombined)
//         .hasSize(1)
//         .allSatisfy(
//             entry ->
//                 assertThat(entry.getData().getSubEntityCounts())
//                     .hasSize(1)
//                     .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(EntityType.REPORT, 2L)));
//   }
//
//   @ParameterizedTest(name = "sortBy={0}, sortOrder={1}")
//   @MethodSource("sortParamsAndExpectedComparator")
//   public void getEntities_resultsAreSortedAccordingToExpectedComparator(
//       String sortBy, SortOrder sortOrder, Comparator<EntityResponseDto> expectedComparator) {
//     // given
//     addCollection("B Collection");
//     addCollection("A Collection");
//     addSingleReportToOptimize("D Report", ReportType.PROCESS);
//     addSingleReportToOptimize("C Report", ReportType.DECISION);
//     addDashboardToOptimize("B Dashboard");
//     addDashboardToOptimize("A Dashboard");
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     EntitySorter sorter = new EntitySorter(sortBy, sortOrder);
//
//     // when
//     final List<EntityResponseDto> allEntities = entitiesClient.getAllEntities(sorter);
//
//     // then
//     assertThat(allEntities).hasSize(6).isSortedAccordingTo(expectedComparator);
//   }
//
//   @Test
//   public void getEntities_unresolvableResultsAreSortedAccordingToDefaultComparator() {
//     // given
//     addCollection("An Entity");
//     addCollection("An Entity");
//     addSingleReportToOptimize("An Entity", ReportType.PROCESS);
//     addSingleReportToOptimize("An Entity", ReportType.DECISION);
//     addDashboardToOptimize("An Entity");
//     addDashboardToOptimize("An Entity");
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     EntitySorter sorter = new EntitySorter(name, SortOrder.ASC);
//     final Comparator<EntityResponseDto> expectedComparator =
//         Comparator.comparing(EntityResponseDto::getName)
//             .thenComparing(EntityResponseDto::getEntityType)
//             .thenComparing(Comparator.comparing(EntityResponseDto::getLastModified).reversed());
//
//     // when
//     final List<EntityResponseDto> allEntities = entitiesClient.getAllEntities(sorter);
//
//     // then
//     assertThat(allEntities).hasSize(6).isSortedAccordingTo(expectedComparator);
//   }
//
//   @Test
//   public void getEntities_resultsAreSortedInAscendingOrderIfNoOrderSupplied() {
//     // given
//     addCollection("A Entity");
//     addCollection("B Entity");
//     addSingleReportToOptimize("C Entity", ReportType.PROCESS);
//     addSingleReportToOptimize("D Entity", ReportType.DECISION);
//     addDashboardToOptimize("E Entity");
//     addDashboardToOptimize("F Entity");
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     EntitySorter sorter = new EntitySorter(name, null);
//     final Comparator<EntityResponseDto> expectedComparator =
//         Comparator.comparing(EntityResponseDto::getName);
//
//     // when
//     final List<EntityResponseDto> allEntities = entitiesClient.getAllEntities(sorter);
//
//     // then
//     assertThat(allEntities).hasSize(6).isSortedAccordingTo(expectedComparator);
//   }
//
//   @Test
//   public void getEntities_invalidSortByParameterPassed() {
//     // given a sortBy field which is not supported
//     EntitySorter sorter = new EntitySorter(EntityResponseDto.Fields.currentUserRole,
// SortOrder.ASC);
//
//     // when
//     final Response response =
//
// embeddedOptimizeExtension.getRequestExecutor().buildGetAllEntitiesRequest(sorter).execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void getEntities_sortOrderSuppliedWithNoSortByField() {
//     // given
//     EntitySorter sorter = new EntitySorter(null, SortOrder.ASC);
//
//     // when
//     final Response response =
//
// embeddedOptimizeExtension.getRequestExecutor().buildGetAllEntitiesRequest(sorter).execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void bulkDeleteEntities_notPossibleForUnauthenticatedUser() {
//     // given
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Arrays.asList("doesntMatter", "doesntMatter"),
//             Arrays.asList("doesntMatter", "doesntMatter"),
//             Arrays.asList("doesntMatter", "doesntMatter"));
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildBulkDeleteEntitiesRequest(entitiesDeleteRequestDto)
//             .withoutAuthentication()
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
//   }
//
//   @Test
//   public void bulkDeleteEntities_forbiddenToDeleteReportsForUnauthorizedUser() {
//     // given
//     String collectionId = collectionClient.createNewCollectionWithDefaultProcessScope();
//     String reportId1 = reportClient.createEmptySingleDecisionReportInCollection(collectionId);
//     String reportId2 = reportClient.createEmptySingleProcessReport();
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Arrays.asList(reportId1, reportId2), Collections.emptyList(),
// Collections.emptyList());
//     authorizationClient.addKermitUserAndGrantAccessToOptimize();
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildBulkDeleteEntitiesRequest(entitiesDeleteRequestDto)
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//   }
//
//   @Test
//   public void bulkDeleteEntities_forbiddenToDeleteDashboardsForUnauthorizedUser() {
//     // given
//     String collectionId = collectionClient.createNewCollectionWithDefaultProcessScope();
//     String dashboardId1 = dashboardClient.createEmptyDashboard(collectionId);
//     String dashboardId2 = dashboardClient.createEmptyDashboard(collectionId);
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(),
//             Collections.emptyList(),
//             Arrays.asList(dashboardId1, dashboardId2));
//     authorizationClient.addKermitUserAndGrantAccessToOptimize();
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildBulkDeleteEntitiesRequest(entitiesDeleteRequestDto)
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//   }
//
//   @Test
//   public void bulkDeleteEntities_forbiddenToDeleteCollectionsForUnauthorizedUser() {
//     // given
//     String collectionId1 = collectionClient.createNewCollectionWithDefaultProcessScope();
//     String collectionId2 = collectionClient.createNewCollectionWithDefaultProcessScope();
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(),
//             Arrays.asList(collectionId1, collectionId2),
//             Collections.emptyList());
//     authorizationClient.addKermitUserAndGrantAccessToOptimize();
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildBulkDeleteEntitiesRequest(entitiesDeleteRequestDto)
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//   }
//
//   @Test
//   public void bulkDeleteEntities_entityListIsEmpty() {
//     // given
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//   }
//
//   @ParameterizedTest
//   @MethodSource("getEntitiesRequestBody")
//   public void bulkDeleteEntities_returnsBadRequestWhenEntityListsAreNull(
//       EntitiesDeleteRequestDto entitiesDeleteRequestDto) {
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void bulkDeleteEntities_deleteEntitiesIsSuccessful() {
//     // given
//     String collectionId1 = collectionClient.createNewCollection();
//     String collectionId2 = collectionClient.createNewCollection();
//     String reportId1 = reportClient.createEmptySingleProcessReport();
//     String reportId2 = reportClient.createEmptySingleProcessReport();
//     String dashBoardId1 =
//         dashboardClient.createDashboard(collectionId1, Collections.singletonList(reportId1));
//     String dashBoardId2 = dashboardClient.createEmptyDashboard(null);
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Arrays.asList(reportId1, reportId2),
//             Arrays.asList(collectionId1, collectionId2),
//             Arrays.asList(dashBoardId1, dashBoardId2));
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(reportClient.getAllReportsAsUser()).isEmpty();
//     assertThat(getAllCollections()).isEmpty();
//     assertThat(getAllDashboards()).isEmpty();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsDashboardsThatCannotBeFound() {
//     // given
//     String dashBoardId1 = dashboardClient.createEmptyDashboard(null);
//     String dashBoardId2 = dashboardClient.createEmptyDashboard(null);
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(),
//             Collections.emptyList(),
//             Arrays.asList(dashBoardId1, "doesntExist", dashBoardId2));
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(getAllDashboards()).isEmpty();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsReportsThatCannotBeFound() {
//     // given
//     String reportId1 = reportClient.createEmptySingleProcessReport();
//     String reportId2 = reportClient.createEmptySingleProcessReport();
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Arrays.asList(reportId1, "doesntExist", reportId2),
//             Collections.emptyList(),
//             Collections.emptyList());
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(reportClient.getAllReportsAsUser()).isEmpty();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsCollectionsThatCannotBeFound() {
//     // given
//     String collectionId1 = collectionClient.createNewCollection();
//     String collectionId2 = collectionClient.createNewCollection();
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(),
//             Arrays.asList(collectionId1, "doesntExist", collectionId2),
//             Collections.emptyList());
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(getAllCollections()).isEmpty();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsEntryWhenEntityIdIsDuplicate() {
//     // given
//     String collectionId1 = collectionClient.createNewCollection();
//     String collectionId2 = collectionClient.createNewCollection();
//     String reportId1 = reportClient.createEmptySingleProcessReport();
//     String reportId2 = reportClient.createEmptySingleProcessReport();
//     String dashboardId1 =
//         dashboardClient.createDashboard(collectionId1, Collections.singletonList(reportId1));
//     String dashboardId2 =
//         dashboardClient.createDashboard(collectionId2, Collections.singletonList(reportId2));
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Arrays.asList(reportId1, reportId1, reportId2),
//             Arrays.asList(collectionId1, collectionId1, collectionId2),
//             Arrays.asList(dashboardId1, dashboardId1, dashboardId2));
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(reportClient.getAllReportsAsUser()).isEmpty();
//     assertThat(getAllDashboards()).isEmpty();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsEntryWhenDBfailsToDeleteReport() {
//     // given
//     String reportId1 = reportClient.createEmptySingleProcessReport();
//     String reportId2 = reportClient.createEmptySingleProcessReport();
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Arrays.asList(reportId1, reportId2), Collections.emptyList(),
// Collections.emptyList());
//
//     final ClientAndServer dbMockServer = useAndGetDbMockServer();
//     final HttpRequest requestMatcher =
//         request()
//             .withPath("/.*" + SINGLE_PROCESS_REPORT_INDEX_NAME + ".*/_delete_by_query")
//             .withMethod(POST);
//     dbMockServer
//         .when(requestMatcher, Times.once())
//         .error(HttpError.error().withDropConnection(true));
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     dbMockServer.verify(requestMatcher, VerificationTimes.atLeast(1));
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(reportClient.getAllReportsAsUser()).hasSize(1);
//     assertThat(reportClient.getReportById(reportId1)).isNotNull();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsEntryWhenDBfailsToDeleteCollection() {
//     // given
//     String collectionId1 = collectionClient.createNewCollection();
//     String collectionId2 = collectionClient.createNewCollection();
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(),
//             Arrays.asList(collectionId1, collectionId2),
//             Collections.emptyList());
//
//     final ClientAndServer dbMockServer = useAndGetDbMockServer();
//     final HttpRequest requestMatcher =
//         request().withPath("/.*" + COLLECTION_INDEX_NAME + ".*").withMethod(DELETE);
//     dbMockServer
//         .when(requestMatcher, Times.once())
//         .error(HttpError.error().withDropConnection(true));
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     dbMockServer.verify(requestMatcher, VerificationTimes.atLeast(1));
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(databaseIntegrationTestExtension.getDocumentCountOf(COLLECTION_INDEX_NAME))
//         .isEqualTo(1);
//     assertThat(collectionClient.getCollectionById(collectionId1)).isNotNull();
//   }
//
//   @Test
//   public void bulkDeleteEntities_skipsEntryWhenDBfailsToDeleteDashboard() {
//     // given
//     String dashboardId1 = dashboardClient.createEmptyDashboard(null);
//     String dashboardId2 = dashboardClient.createEmptyDashboard(null);
//
//     EntitiesDeleteRequestDto entitiesDeleteRequestDto =
//         new EntitiesDeleteRequestDto(
//             Collections.emptyList(),
//             Collections.emptyList(),
//             Arrays.asList(dashboardId1, dashboardId2));
//
//     final ClientAndServer dbMockServer = useAndGetDbMockServer();
//     final HttpRequest requestMatcher =
//         request().withPath("/.*" + DASHBOARD_INDEX_NAME + ".*").withMethod(DELETE);
//     dbMockServer
//         .when(requestMatcher, Times.once())
//         .error(HttpError.error().withDropConnection(true));
//
//     // when
//     Response response = buildAndExecuteBulkDeleteEntitiesRequest(entitiesDeleteRequestDto);
//
//     // then
//     dbMockServer.verify(requestMatcher, VerificationTimes.atLeast(1));
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(databaseIntegrationTestExtension.getDocumentCountOf(DASHBOARD_INDEX_NAME))
//         .isEqualTo(1);
//     assertThat(dashboardClient.getDashboard(dashboardId1)).isNotNull();
//   }
//
//   private void addRoleToCollection(
//       final String collectionId, final String identityId, final IdentityType identityType) {
//
//     final CollectionRoleRequestDto roleDto =
//         new CollectionRoleRequestDto(
//             identityType.equals(IdentityType.USER)
//                 ? new IdentityDto(identityId, IdentityType.USER)
//                 : new IdentityDto(identityId, IdentityType.GROUP),
//             RoleType.EDITOR);
//     collectionClient.addRolesToCollection(collectionId, roleDto);
//   }
//
//   private Response buildAndExecuteBulkDeleteEntitiesRequest(
//       EntitiesDeleteRequestDto entitiesDeleteRequestDto) {
//     return embeddedOptimizeExtension
//         .getRequestExecutor()
//         .buildBulkDeleteEntitiesRequest(entitiesDeleteRequestDto)
//         .execute();
//   }
//
//   private List<CollectionDefinitionDto> getAllCollections() {
//     return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
//         COLLECTION_INDEX_NAME, CollectionDefinitionDto.class);
//   }
//
//   private List<DashboardDefinitionRestDto> getAllDashboards() {
//     return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
//         DASHBOARD_INDEX_NAME, DashboardDefinitionRestDto.class);
//   }
//
//   private static Stream<EntitiesDeleteRequestDto> getEntitiesRequestBody() {
//     return Stream.of(
//         new EntitiesDeleteRequestDto(null, Collections.emptyList(), Collections.emptyList()),
//         new EntitiesDeleteRequestDto(Collections.emptyList(), null, Collections.emptyList()),
//         new EntitiesDeleteRequestDto(Collections.emptyList(), Collections.emptyList(), null),
//         null);
//   }
//
//   private void addSingleReportWithDescriptionToOptimize(
//       final String name, final ReportType reportType, final String description) {
//     addSingleReportToOptimize(name, description, reportType, null, DEFAULT_USERNAME);
//   }
//
//   private void addDashboardWithDescriptionToOptimize(final String name, final String description)
// {
//     addDashboardToOptimize(name, description, null, DEFAULT_USERNAME);
//   }
// }
