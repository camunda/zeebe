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
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
// import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
// import io.camunda.optimize.dto.optimize.rest.sorting.EntitySorter;
// import jakarta.ws.rs.core.Response;
// import java.util.Comparator;
// import java.util.List;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
//
// @Tag(OPENSEARCH_PASSING)
// public class CollectionEntitiesRestServiceIT extends AbstractEntitiesRestServiceIT {
//
//   public static final Comparator<EntityResponseDto> DEFAULT_ENTITIES_COMPARATOR =
//       Comparator.comparing(EntityResponseDto::getName)
//           .thenComparing(Comparator.comparing(EntityResponseDto::getLastModified).reversed());
//
//   @Test
//   public void getCollectionEntitiesWithoutAuthentication() {
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetCollectionEntitiesRequest("collectionId")
//             .withoutAuthentication()
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
//   }
//
//   @Test
//   public void getCollectionEntities_sortedByDefaultComparator() {
//     // given
//     final String collectionId = createCollectionWithMixedEntities();
//
//     // when
//     final List<EntityResponseDto> collectionEntities =
//         collectionClient.getEntitiesForCollection(collectionId);
//
//     // then
//     assertThat(collectionEntities).hasSize(4).isSortedAccordingTo(DEFAULT_ENTITIES_COMPARATOR);
//   }
//
//   @ParameterizedTest(name = "sortBy={0}, sortOrder={1}")
//   @MethodSource("sortParamsAndExpectedComparator")
//   public void getCollectionEntities_resultsAreSortedAccordingToExpectedComparator(
//       String sortBy, SortOrder sortOrder, Comparator<EntityResponseDto> expectedComparator) {
//     // given
//     final String collectionId = createCollectionWithMixedEntities();
//     EntitySorter sorter = new EntitySorter(sortBy, sortOrder);
//
//     // when
//     final List<EntityResponseDto> collectionEntities =
//         collectionClient.getEntitiesForCollection(collectionId, sorter);
//
//     // then
//     assertThat(collectionEntities)
//         .hasSize(4)
//         .isSortedAccordingTo(expectedComparator.thenComparing(DEFAULT_ENTITIES_COMPARATOR));
//   }
//
//   @Test
//   public void getCollectionEntities_resultsAreSortedInAscendingOrderIfNoOrderSupplied() {
//     // given
//     final String collectionId = createCollectionWithMixedEntities();
//     EntitySorter sorter = new EntitySorter("name", null);
//
//     // when
//     final List<EntityResponseDto> collectionEntities =
//         collectionClient.getEntitiesForCollection(collectionId, sorter);
//
//     // then
//     assertThat(collectionEntities)
//         .hasSize(4)
//         .isSortedAccordingTo(Comparator.comparing(EntityResponseDto::getName));
//   }
//
//   @Test
//   public void getCollectionEntities_invalidSortByParameterPassed() {
//     // given a sortBy field which is not supported
//     final String collectionId = createCollectionWithMixedEntities();
//     EntitySorter sorter = new EntitySorter(EntityResponseDto.Fields.currentUserRole,
// SortOrder.ASC);
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetCollectionEntitiesRequest(collectionId, sorter)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void getCollectionEntities_sortOrderSuppliedWithNoSortByField() {
//     // given a sortBy field which is not supported
//     final String collectionId = createCollectionWithMixedEntities();
//     EntitySorter sorter = new EntitySorter(null, SortOrder.ASC);
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetCollectionEntitiesRequest(collectionId, sorter)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   private String createCollectionWithMixedEntities() {
//     final String collectionId = collectionClient.createNewCollection();
//     reportClient.createEmptySingleProcessReportInCollection(collectionId);
//     reportClient.createEmptySingleDecisionReportInCollection(collectionId);
//     reportClient.createEmptyCombinedReport(collectionId);
//     dashboardClient.createEmptyDashboard(collectionId);
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//     return collectionId;
//   }
// }
