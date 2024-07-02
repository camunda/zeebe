/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest.sorting;

import static io.camunda.optimize.dto.optimize.query.event.sequence.EventCountResponseDto.Fields.count;
import static io.camunda.optimize.dto.optimize.query.event.sequence.EventCountResponseDto.Fields.eventName;
import static io.camunda.optimize.dto.optimize.query.event.sequence.EventCountResponseDto.Fields.group;
import static io.camunda.optimize.dto.optimize.query.event.sequence.EventCountResponseDto.Fields.source;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.dto.optimize.query.event.sequence.EventCountResponseDto;
import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import jakarta.ws.rs.BadRequestException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EventCountSorter extends Sorter<EventCountResponseDto> {

  private static final Comparator<EventCountResponseDto> SUGGESTED_COMPARATOR =
      Comparator.comparing(EventCountResponseDto::isSuggested, nullsFirst(naturalOrder()))
          .reversed();
  private static final Comparator<EventCountResponseDto> GROUP_COMPARATOR =
      Comparator.comparing(
          EventCountResponseDto::getGroup, nullsFirst(String.CASE_INSENSITIVE_ORDER));
  private static final Comparator<EventCountResponseDto> SOURCE_COMPARATOR =
      Comparator.comparing(
          EventCountResponseDto::getSource, nullsFirst(String.CASE_INSENSITIVE_ORDER));
  private static final Comparator<EventCountResponseDto> EVENT_NAME_COMPARATOR =
      Comparator.comparing(
          eventCountDto ->
              Optional.ofNullable(eventCountDto.getEventLabel())
                  .orElse(eventCountDto.getEventName()),
          nullsFirst(String.CASE_INSENSITIVE_ORDER));
  private static final Comparator<EventCountResponseDto> COUNTS_COMPARATOR =
      Comparator.comparing(EventCountResponseDto::getCount, nullsFirst(naturalOrder()));

  private static final Comparator<EventCountResponseDto> DEFAULT_COMPARATOR =
      nullsFirst(
          GROUP_COMPARATOR
              .thenComparing(SOURCE_COMPARATOR)
              .thenComparing(EVENT_NAME_COMPARATOR)
              .thenComparing(COUNTS_COMPARATOR));

  private static final ImmutableMap<String, Comparator<EventCountResponseDto>> sortComparators =
      ImmutableMap.of(
          group.toLowerCase(Locale.ENGLISH), GROUP_COMPARATOR,
          source.toLowerCase(Locale.ENGLISH), SOURCE_COMPARATOR,
          eventName.toLowerCase(Locale.ENGLISH), EVENT_NAME_COMPARATOR,
          count.toLowerCase(Locale.ENGLISH), COUNTS_COMPARATOR);

  public EventCountSorter(final String sortBy, final SortOrder sortOrder) {
    this.sortRequestDto = new SortRequestDto(sortBy, sortOrder);
  }

  @Override
  public List<EventCountResponseDto> applySort(List<EventCountResponseDto> eventCounts) {
    Comparator<EventCountResponseDto> eventCountSorter;
    final Optional<SortOrder> sortOrderOpt = getSortOrder();
    final Optional<String> sortByOpt = getSortBy();
    if (sortByOpt.isPresent()) {
      final String sortBy = sortByOpt.get();
      if (!sortComparators.containsKey(sortBy.toLowerCase(Locale.ENGLISH))) {
        throw new BadRequestException(String.format("%s is not a sortable field", sortBy));
      }
      eventCountSorter =
          sortComparators.get(sortBy.toLowerCase(Locale.ENGLISH)).thenComparing(DEFAULT_COMPARATOR);
      if (sortOrderOpt.isPresent() && SortOrder.DESC.equals(sortOrderOpt.get())) {
        eventCountSorter = eventCountSorter.reversed();
      }
    } else {
      if (sortOrderOpt.isPresent()) {
        throw new BadRequestException("Sort order is not supported when no field selected to sort");
      }
      eventCountSorter = DEFAULT_COMPARATOR;
    }
    eventCounts.sort(SUGGESTED_COMPARATOR.thenComparing(eventCountSorter));
    return eventCounts;
  }
}
