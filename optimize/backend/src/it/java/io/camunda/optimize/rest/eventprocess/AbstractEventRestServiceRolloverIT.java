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
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.dto.optimize.query.event.DeletableEventDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
// // import io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
// // import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
// // import java.time.Instant;
// // import java.util.Collections;
// // import java.util.List;
// // import org.junit.jupiter.api.BeforeEach;
// //
// // public abstract class AbstractEventRestServiceRolloverIT extends AbstractEventProcessIT {
// //
// //   protected static final String TIMESTAMP = DeletableEventDto.Fields.timestamp;
// //   protected static final String GROUP = DeletableEventDto.Fields.group;
// //
// //   protected CloudEventRequestDto impostorSabotageNav =
// //       createEventDtoWithProperties("impostors", "navigationRoom", "sabotage", Instant.now());
// //
// //   protected CloudEventRequestDto impostorMurderedMedBay =
// //       createEventDtoWithProperties(
// //           "impostors", "medBay", "murderedNormie", Instant.now().plusSeconds(1));
// //
// //   protected CloudEventRequestDto normieTaskNav =
// //       createEventDtoWithProperties(
// //           "normie", "navigationRoom", "finishedTask", Instant.now().plusSeconds(2));
// //
// //   @BeforeEach
// //   public void cleanUpEventIndices() {
// //     databaseIntegrationTestExtension.deleteAllExternalEventIndices();
// //     embeddedOptimizeExtension
// //         .getDatabaseSchemaManager()
// //         .createOrUpdateOptimizeIndex(
// //             embeddedOptimizeExtension.getOptimizeDatabaseClient(),
// //             databaseIntegrationTestExtension.getEventIndex());
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getEventIndexRolloverConfiguration()
// //         .setMaxIndexSizeGB(0);
// //     embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
// //     embeddedOptimizeExtension.reloadConfiguration();
// //   }
// //
// //   protected void ingestEventAndRolloverIndex(final CloudEventRequestDto cloudEventRequestDto)
// {
// //     ingestionClient.ingestEventBatch(Collections.singletonList(cloudEventRequestDto));
// //     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
// //     embeddedOptimizeExtension.getEventIndexRolloverService().triggerRollover();
// //   }
// //
// //   protected CloudEventRequestDto createEventDtoWithProperties(
// //       final String group, final String source, final String type, final Instant timestamp) {
// //     return ingestionClient.createCloudEventDto().toBuilder()
// //         .group(group)
// //         .source(source)
// //         .type(type)
// //         .time(timestamp)
// //         .build();
// //   }
// //
// //   protected void assertThatEventsHaveBeenDeleted(
// //       final List<EventDto> allSavedEventsBeforeDelete, final List<String>
// expectedDeletedEvenIds) {
// //     assertThat(getAllStoredEvents())
// //         .hasSize(allSavedEventsBeforeDelete.size() - expectedDeletedEvenIds.size())
// //         .extracting(EventDto::getId)
// //         .doesNotContainAnyElementsOf(expectedDeletedEvenIds);
// //   }
// // }
