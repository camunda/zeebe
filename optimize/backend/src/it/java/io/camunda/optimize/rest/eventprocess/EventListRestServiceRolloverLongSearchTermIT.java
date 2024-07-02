/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.eventprocess;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.query.sorting.SortOrder.DESC;
import static java.util.Comparator.naturalOrder;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.event.DeletableEventDto;
import io.camunda.optimize.dto.optimize.query.event.EventSearchRequestDto;
import io.camunda.optimize.dto.optimize.rest.Page;
import io.camunda.optimize.dto.optimize.rest.pagination.PaginationRequestDto;
import io.camunda.optimize.dto.optimize.rest.sorting.SortRequestDto;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class EventListRestServiceRolloverLongSearchTermIT
    extends AbstractEventRestServiceRolloverIT {

  @Test
  public void getEventCountsWithRolledOverEventIndices_longSearchTermMatchesPrefix() {
    // given an event for each index
    ingestEventAndRolloverIndex(impostorSabotageNav);
    ingestEventAndRolloverIndex(impostorMurderedMedBay);
    ingestEventAndRolloverIndex(normieTaskNav);
    final String searchTerm = "navigationRo";

    // when
    final Page<DeletableEventDto> eventsPage =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetEventListRequest(
                new EventSearchRequestDto(
                    searchTerm,
                    new SortRequestDto(TIMESTAMP, DESC),
                    new PaginationRequestDto(20, 0)))
            .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());

    // then only the results matching the search term are included
    assertThat(eventsPage.getSortBy()).isEqualTo(TIMESTAMP);
    assertThat(eventsPage.getSortOrder()).isEqualTo(DESC);
    assertThat(eventsPage.getTotal()).isEqualTo(2);
    assertThat(eventsPage.getResults())
        .isSortedAccordingTo(
            Comparator.comparing(
                    DeletableEventDto::getTimestamp, Comparator.nullsFirst(naturalOrder()))
                .reversed())
        .hasSize(2)
        .extracting(DeletableEventDto::getId)
        .containsExactly(normieTaskNav.getId(), impostorSabotageNav.getId());
  }

  @Test
  public void getEventCountsWithRolledOverEventIndices_longSearchTermDoesNotMatchPrefix() {
    // given an event for each index
    ingestEventAndRolloverIndex(impostorSabotageNav);
    ingestEventAndRolloverIndex(impostorMurderedMedBay);
    ingestEventAndRolloverIndex(normieTaskNav);
    final String searchTerm = "vigationRoom";

    // when
    final Page<DeletableEventDto> eventsPage =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetEventListRequest(
                new EventSearchRequestDto(
                    searchTerm,
                    new SortRequestDto(TIMESTAMP, DESC),
                    new PaginationRequestDto(20, 0)))
            .executeAndGetPage(DeletableEventDto.class, Response.Status.OK.getStatusCode());

    // then no results are returned
    assertThat(eventsPage.getSortBy()).isEqualTo(TIMESTAMP);
    assertThat(eventsPage.getSortOrder()).isEqualTo(DESC);
    assertThat(eventsPage.getTotal()).isZero();
    assertThat(eventsPage.getResults()).isEmpty();
  }
}
