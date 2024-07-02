/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.eventprocess;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class EventDeleteRestServiceRolloverNotUsedInProcessInstanceIT
    extends AbstractEventRestServiceRolloverIT {

  @Test
  public void deleteRolledOverEvents_deleteSingleEventNotUsedInEventInstance() {
    // given an event for each index
    ingestEventAndRolloverIndex(impostorSabotageNav);
    ingestEventAndRolloverIndex(impostorMurderedMedBay);
    ingestEventAndRolloverIndex(normieTaskNav);
    final List<EventDto> savedEventsBeforeDelete = getAllStoredEvents();
    final List<String> eventIdsToDelete =
        Collections.singletonList(savedEventsBeforeDelete.get(0).getId());

    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteEventsRequest(eventIdsToDelete)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThatEventsHaveBeenDeleted(savedEventsBeforeDelete, eventIdsToDelete);
  }
}
