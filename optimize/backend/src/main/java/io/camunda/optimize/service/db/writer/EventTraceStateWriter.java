/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.writer;

import io.camunda.optimize.dto.optimize.query.event.sequence.EventTraceStateDto;
import io.camunda.optimize.service.db.schema.index.events.EventTraceStateIndex;
import java.util.List;

public interface EventTraceStateWriter {

  void upsertEventTraceStates(final List<EventTraceStateDto> eventTraceStateDtos);

  default String updateScript() {
    return """
              for (def tracedEvent : params.eventTrace) {
                  ctx._source.eventTrace.removeIf(event -> event.eventId.equals(tracedEvent.eventId));
              }
              ctx._source.eventTrace.addAll(params.eventTrace);
            """;
  }

  default String getIndexName(String indexKey) {
    return EventTraceStateIndex.constructIndexName(indexKey);
  }
}
