/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// // TODO recreate C8 IT equivalent of this with #13337
// // package io.camunda.optimize.rest.eventprocess;
// //
// // import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// // import static io.camunda.optimize.dto.optimize.query.sorting.SortOrder.ASC;
// // import static io.camunda.optimize.dto.optimize.query.sorting.SortOrder.DESC;
// // import static java.util.Comparator.naturalOrder;
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.dto.optimize.query.event.DeletableEventDto;
// // import io.camunda.optimize.dto.optimize.query.event.EventSearchRequestDto;
// // import io.camunda.optimize.dto.optimize.rest.Page;
// // import io.camunda.optimize.dto.optimize.rest.pagination.PaginationRequestDto;
// // import io.camunda.optimize.dto.optimize.rest.sorting.SortRequestDto;
// // import jakarta.ws.rs.core.Response;
// // import java.util.Comparator;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class EventListRestServiceRolloverIT extends AbstractEventRestServiceRolloverIT {
// //
// //   @Test
// //   public void getEventCountsWithRolledOverEventIndices_noSearchTerm() {
// //     // given an event for each index
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //
// //     // when
// //     final Page<DeletableEventDto> eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     null, new SortRequestDto(GROUP, ASC), new PaginationRequestDto(20, 0)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then the results from all indices return sorted by parameters
// //     assertThat(eventsPage.getSortBy()).isEqualTo(GROUP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(ASC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(3);
// //     assertThat(eventsPage.getResults())
// //         .isSortedAccordingTo(
// //             Comparator.comparing(DeletableEventDto::getGroup,
// Comparator.nullsFirst(naturalOrder()))
// //
// .thenComparing(Comparator.comparing(DeletableEventDto::getTimestamp).reversed()))
// //         .hasSize(3)
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(
// //             impostorMurderedMedBay.getId(), impostorSabotageNav.getId(),
// normieTaskNav.getId());
// //   }
// //
// //   @Test
// //   public void getEventCountsWithRolledOverEventIndices_noSearchTerm_orSortParams() {
// //     // given an event for each index
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //
// //     // when
// //     final Page<DeletableEventDto> eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     null, new SortRequestDto(null, null), new PaginationRequestDto(20, 0)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then the results from all indices return sorted by default property
// //     assertThat(eventsPage.getSortBy()).isEqualTo(TIMESTAMP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(DESC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(3);
// //     assertThat(eventsPage.getResults())
// //         .isSortedAccordingTo(
// //             Comparator.comparing(
// //                     DeletableEventDto::getTimestamp, Comparator.nullsFirst(naturalOrder()))
// //                 .reversed()
// //
// .thenComparing(Comparator.comparing(DeletableEventDto::getTimestamp).reversed()))
// //         .hasSize(3)
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(
// //             normieTaskNav.getId(), impostorMurderedMedBay.getId(),
// impostorSabotageNav.getId());
// //   }
// //
// //   @Test
// //   public void getEventCountsWithRolledOverEventIndices_searchTermMatchingExactly() {
// //     // given an event for each index
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //
// //     // when
// //     final Page<DeletableEventDto> eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     "impostors",
// //                     new SortRequestDto(TIMESTAMP, DESC),
// //                     new PaginationRequestDto(20, 0)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then only the results matching the search term are included
// //     assertThat(eventsPage.getSortBy()).isEqualTo(TIMESTAMP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(DESC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(2);
// //     assertThat(eventsPage.getResults())
// //         .isSortedAccordingTo(
// //             Comparator.comparing(
// //                     DeletableEventDto::getTimestamp, Comparator.nullsFirst(naturalOrder()))
// //                 .reversed())
// //         .hasSize(2)
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(impostorMurderedMedBay.getId(), impostorSabotageNav.getId());
// //   }
// //
// //   @Test
// //   public void getEventCountsWithRolledOverEventIndices_searchTermMatchesContains() {
// //     // given an event for each index
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //
// //     // when
// //     final Page<DeletableEventDto> eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     "impost", new SortRequestDto(TIMESTAMP, DESC), new
// PaginationRequestDto(20, 0)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then only the results matching the search term are included
// //     assertThat(eventsPage.getSortBy()).isEqualTo(TIMESTAMP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(DESC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(2);
// //     assertThat(eventsPage.getResults())
// //         .isSortedAccordingTo(
// //             Comparator.comparing(
// //                     DeletableEventDto::getTimestamp, Comparator.nullsFirst(naturalOrder()))
// //                 .reversed())
// //         .hasSize(2)
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(impostorMurderedMedBay.getId(), impostorSabotageNav.getId());
// //   }
// //
// //   @Test
// //   public void getEventCountsWithRolledOverEventIndices_paginateThroughResults() {
// //     // given an event for each index
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //
// //     // when I request the first page
// //     Page<DeletableEventDto> eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     null, new SortRequestDto(GROUP, ASC), new PaginationRequestDto(1, 0)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then the first page contains only the first event according to sort parameters
// //     assertThat(eventsPage.getSortBy()).isEqualTo(GROUP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(ASC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(3);
// //     assertThat(eventsPage.getResults())
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(impostorMurderedMedBay.getId());
// //
// //     // when I request the second page
// //     eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     null, new SortRequestDto(GROUP, ASC), new PaginationRequestDto(1, 1)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then the second page contains only the second event according to sort parameters
// //     assertThat(eventsPage.getSortBy()).isEqualTo(GROUP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(ASC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(3);
// //     assertThat(eventsPage.getResults())
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(impostorSabotageNav.getId());
// //
// //     // when I request the second page
// //     eventsPage =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventListRequest(
// //                 new EventSearchRequestDto(
// //                     null, new SortRequestDto(GROUP, ASC), new PaginationRequestDto(1, 2)))
// //             .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());
// //
// //     // then the third page contains only the third event according to sort parameters
// //     assertThat(eventsPage.getSortBy()).isEqualTo(GROUP);
// //     assertThat(eventsPage.getSortOrder()).isEqualTo(ASC);
// //     assertThat(eventsPage.getTotal()).isEqualTo(3);
// //     assertThat(eventsPage.getResults())
// //         .extracting(DeletableEventDto::getId)
// //         .containsExactly(normieTaskNav.getId());
// //   }
// // }
