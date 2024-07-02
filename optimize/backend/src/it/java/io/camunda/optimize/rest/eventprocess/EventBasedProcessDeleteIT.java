/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.eventprocess;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.DefinitionType.PROCESS;
import static io.camunda.optimize.rest.eventprocess.EventBasedProcessRestServiceIT.createProcessDefinitionXml;
import static io.camunda.optimize.service.db.DatabaseConstants.COLLECTION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.EVENT_PROCESS_PUBLISH_STATE_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.VARIABLE_LABEL_INDEX_NAME;
import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_DEFINITION_KEY;
import static io.camunda.optimize.test.optimize.EventProcessClient.createEventMappingsDto;
import static io.camunda.optimize.test.optimize.EventProcessClient.createMappedEventDto;
import static jakarta.ws.rs.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDataDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardReportTileDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessPublishStateDto;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.variable.DefinitionVariableLabelsDto;
import io.camunda.optimize.dto.optimize.query.variable.LabelDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionResponseDto;
import io.camunda.optimize.service.EventProcessService;
import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
import io.github.netmikey.logunit.api.LogCapturer;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.event.Level;

@Tag(OPENSEARCH_PASSING)
public class EventBasedProcessDeleteIT extends AbstractEventProcessIT {

  private static String simpleDiagramXml;

  final LabelDto FIRST_LABEL = new LabelDto("first label", "a name", VariableType.STRING);
  final LabelDto SECOND_LABEL = new LabelDto("second label", "a name", VariableType.STRING);

  @RegisterExtension
  @Order(5)
  protected final LogCapturer logCapturer =
      LogCapturer.create().forLevel(Level.DEBUG).captureForType(EventProcessService.class);

  @BeforeAll
  public static void setup() {
    simpleDiagramXml = createProcessDefinitionXml();
  }

  @Test
  public void deleteEventProcessMapping() {
    // given
    String storedEventProcessMappingId =
        eventProcessClient.createEventProcessMapping(
            eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml));

    // when
    Response response =
        eventProcessClient
            .createBulkDeleteEventProcessMappingsRequest(List.of(storedEventProcessMappingId))
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertGetMappingRequestStatusCode(
        storedEventProcessMappingId, Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void bulkDeleteEventProcessMappings_notPossibleForUnauthenticatedUser() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .bulkDeleteEventProcessMappingsRequest(Arrays.asList("someId", "someOtherId"))
            .withoutAuthentication()
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void bulkDeleteEventProcessMappings_forbiddenForUnauthorizedUser() {
    // when
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .bulkDeleteEventProcessMappingsRequest(Arrays.asList("someId", "someOtherId"))
            .withUserAuthentication(KERMIT_USER, KERMIT_USER)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void bulkDeleteEventProcessMappings_emptyEventBasedProcessIdList() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .bulkDeleteEventProcessMappingsRequest(Collections.emptyList())
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  public void bulkDeleteEventProcessMappings_nullEventBasedProcessIdList() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .bulkDeleteEventProcessMappingsRequest(null)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void bulkDeleteEventProcessMappings() {
    // given
    EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
    String eventProcessDefinitionKey1 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    String eventProcessDefinitionKey2 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    List<String> eventProcessMapings =
        Arrays.asList(eventProcessDefinitionKey1, eventProcessDefinitionKey2);

    // when
    Response response =
        eventProcessClient
            .createBulkDeleteEventProcessMappingsRequest(eventProcessMapings)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey1, Response.Status.NOT_FOUND.getStatusCode());
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey2, Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void bulkDeletePublishedEventProcessMappings() {
    // given
    EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
    String eventProcessId1 = eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    publishMappingAndExecuteImport(eventProcessId1);
    final EventProcessPublishStateDto publishState1 =
        getEventProcessPublishStateDto(eventProcessId1);
    assertThat(eventInstanceIndexForPublishStateExists(publishState1)).isTrue();
    String eventProcessId2 = eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    publishMappingAndExecuteImport(eventProcessId2);
    final EventProcessPublishStateDto publishState2 =
        getEventProcessPublishStateDto(eventProcessId2);
    assertThat(eventInstanceIndexForPublishStateExists(publishState2)).isTrue();

    List<String> eventBasedProcesses = Arrays.asList(eventProcessId1, eventProcessId2);

    // when
    eventProcessClient.createBulkDeleteEventProcessMappingsRequest(eventBasedProcesses).execute();
    embeddedOptimizeExtension.getEventBasedProcessesInstanceImportScheduler().runImportRound();

    // then
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessId1)).isEmpty();
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessId2)).isEmpty();
    assertThat(eventInstanceIndexForPublishStateExists(publishState1)).isFalse();
    assertThat(eventInstanceIndexForPublishStateExists(publishState2)).isFalse();
    assertGetMappingRequestStatusCode(eventProcessId1, Response.Status.NOT_FOUND.getStatusCode());
    assertGetMappingRequestStatusCode(eventProcessId2, Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void eventProcessMappingsSkippedOnBulkDelete_IfDatabaseFailsToDeleteReportsUsingMapping() {
    EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
    String eventProcessDefinitionKey1 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    String eventProcessDefinitionKey2 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey1);
    eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey2);
    String reportUsingMapping =
        reportClient.createAndStoreProcessReport(eventProcessDefinitionKey1);
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request()
            .withPath("/.*" + SINGLE_PROCESS_REPORT_INDEX_NAME + ".*/_delete_by_query")
            .withMethod(POST);
    dbMockServer
        .when(requestMatcher, Times.once())
        .error(HttpError.error().withDropConnection(true));

    List<String> eventProcessIds =
        Arrays.asList(eventProcessDefinitionKey1, eventProcessDefinitionKey2);

    // when
    Response response =
        eventProcessClient.createBulkDeleteEventProcessMappingsRequest(eventProcessIds).execute();

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.once());
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey1, Response.Status.OK.getStatusCode());
    assertThat(reportClient.getReportById(reportUsingMapping)).isNotNull();
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey2, Response.Status.NOT_FOUND.getStatusCode());
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessDefinitionKey1)).isNotEmpty();
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessDefinitionKey2)).isEmpty();
    logCapturer.assertContains(
        "There was an error while deleting resources associated to the event process mapping with id "
            + eventProcessDefinitionKey1);
  }

  @Test
  public void bulkDeleteEventProcessMappings_skipsDeletionWhenEventProcessMappingDoesNotExist() {
    // given
    EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
    String eventProcessDefinitionKey1 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    String eventProcessDefinitionKey2 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    // when
    List<String> eventProcessIds =
        Arrays.asList(eventProcessDefinitionKey1, "doesNotExist1", eventProcessDefinitionKey2);
    Response response =
        eventProcessClient.createBulkDeleteEventProcessMappingsRequest(eventProcessIds).execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey1, Response.Status.NOT_FOUND.getStatusCode());
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey2, Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void bulkDeleteEventProcessMappings_skipsDeletionIfDatabaseFailsToDeletePublishState() {
    // given
    EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
    String eventProcessDefinitionKey1 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    String eventProcessDefinitionKey2 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey1);

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request()
            .withPath("/.*-" + EVENT_PROCESS_PUBLISH_STATE_INDEX_NAME + "/_update_by_query")
            .withMethod(POST);
    dbMockServer
        .when(requestMatcher, Times.once())
        .error(HttpError.error().withDropConnection(true));

    List<String> eventProcessIds =
        Arrays.asList(eventProcessDefinitionKey1, eventProcessDefinitionKey2);

    // when
    Response response =
        eventProcessClient.createBulkDeleteEventProcessMappingsRequest(eventProcessIds).execute();

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.exactly(2));
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey1, Response.Status.OK.getStatusCode());
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessDefinitionKey1)).isNotEmpty();
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey2, Response.Status.NOT_FOUND.getStatusCode());
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessDefinitionKey2)).isEmpty();
    logCapturer.assertContains(
        "There was an error while deleting resources associated to the event process mapping with id "
            + eventProcessDefinitionKey1);
  }

  @Test
  public void
      bulkDeleteEventProcessMapping_skipsDeletionIfDatabaseFailsToDeleteMappingAsScopeEntry() {
    // given
    EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
    String eventProcessDefinitionKey1 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    String eventProcessDefinitionKey2 =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey1);
    String collectionId = collectionClient.createNewCollectionWithDefaultProcessScope();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    collectionClient.addScopeEntryToCollection(
        collectionId, new CollectionScopeEntryDto(PROCESS, eventProcessDefinitionKey1));

    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    final HttpRequest requestMatcher =
        request().withPath("/.*" + COLLECTION_INDEX_NAME + ".*/_update_by_query").withMethod(POST);
    dbMockServer
        .when(requestMatcher, Times.once())
        .error(HttpError.error().withDropConnection(true));

    List<String> eventProcessIds =
        Arrays.asList(eventProcessDefinitionKey1, eventProcessDefinitionKey2);

    // when
    eventProcessClient.createBulkDeleteEventProcessMappingsRequest(eventProcessIds).execute();

    // then
    dbMockServer.verify(requestMatcher, VerificationTimes.exactly(2));
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey1, Response.Status.OK.getStatusCode());
    assertThat(collectionClient.getCollectionById(collectionId).getData().getScope())
        .extracting(CollectionScopeEntryDto::getDefinitionKey)
        .contains(eventProcessDefinitionKey1);
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessDefinitionKey1)).isNotEmpty();
    assertGetMappingRequestStatusCode(
        eventProcessDefinitionKey2, Response.Status.NOT_FOUND.getStatusCode());
    assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessDefinitionKey2)).isEmpty();
    logCapturer.assertContains(
        "There was an error while deleting resources associated to the event process mapping with id "
            + eventProcessDefinitionKey1);
  }

  @Test
  public void bulkDeletePublishedEventProcessMappings_dependentResourcesGetCleared() {
    // given
    ingestTestEvent(STARTED_EVENT);
    ingestTestEvent(FINISHED_EVENT);
    final EventProcessMappingDto simpleEventProcessMappingDto =
        buildSimpleEventProcessMappingDto(STARTED_EVENT, FINISHED_EVENT);
    String firstDeletingEventProcessDefinitionKey =
        eventProcessClient.createEventProcessMapping(simpleEventProcessMappingDto);
    String secondDeletingEventProcessDefinitionKey =
        eventProcessClient.createEventProcessMapping(simpleEventProcessMappingDto);
    String nonDeletedEventProcessDefinitionKey =
        eventProcessClient.createEventProcessMapping(simpleEventProcessMappingDto);

    publishMappingAndExecuteImport(firstDeletingEventProcessDefinitionKey);
    publishMappingAndExecuteImport(secondDeletingEventProcessDefinitionKey);
    publishMappingAndExecuteImport(nonDeletedEventProcessDefinitionKey);
    EventProcessPublishStateDto publishState =
        getEventProcessPublishStateDto(firstDeletingEventProcessDefinitionKey);
    assertThat(eventInstanceIndexForPublishStateExists(publishState)).isTrue();
    executeImportCycle();

    String collectionId = collectionClient.createNewCollectionWithDefaultProcessScope();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    addLabelForEventProcessDefinition(firstDeletingEventProcessDefinitionKey, FIRST_LABEL);
    DefinitionVariableLabelsDto nonDeletedDefinitionVariableLabelsDto =
        addLabelForEventProcessDefinition(nonDeletedEventProcessDefinitionKey, SECOND_LABEL);

    collectionClient.addScopeEntryToCollection(
        collectionId, new CollectionScopeEntryDto(PROCESS, firstDeletingEventProcessDefinitionKey));

    String reportWithEventProcessDefKey =
        reportClient.createSingleProcessReport(
            reportClient.createSingleProcessReportDefinitionDto(
                collectionId, firstDeletingEventProcessDefinitionKey, Collections.emptyList()));
    String reportIdWithDefaultDefKey =
        reportClient.createSingleProcessReport(
            reportClient.createSingleProcessReportDefinitionDto(
                collectionId, DEFAULT_DEFINITION_KEY, Collections.emptyList()));
    String reportIdWithNoDefKey =
        reportClient.createSingleProcessReport(
            reportClient.createSingleProcessReportDefinitionDto(
                collectionId, Collections.emptyList()));
    reportClient.createCombinedReport(
        collectionId, Arrays.asList(reportWithEventProcessDefKey, reportIdWithDefaultDefKey));

    alertClient.createAlertForReport(reportWithEventProcessDefKey);
    String alertIdToRemain = alertClient.createAlertForReport(reportIdWithDefaultDefKey);

    String dashboardId =
        dashboardClient.createDashboard(
            collectionId,
            Arrays.asList(
                reportWithEventProcessDefKey, reportIdWithDefaultDefKey, reportIdWithNoDefKey));

    List<String> eventProcessIds =
        Arrays.asList(
            firstDeletingEventProcessDefinitionKey, secondDeletingEventProcessDefinitionKey);

    // when
    eventProcessClient.createBulkDeleteEventProcessMappingsRequest(eventProcessIds).execute();
    embeddedOptimizeExtension.getEventBasedProcessesInstanceImportScheduler().runImportRound();

    // then
    assertGetMappingRequestStatusCode(
        firstDeletingEventProcessDefinitionKey, Response.Status.NOT_FOUND.getStatusCode());
    assertThat(collectionClient.getReportsForCollection(collectionId))
        .extracting(
            AuthorizedReportDefinitionResponseDto.Fields.definitionDto
                + "."
                + ReportDefinitionDto.Fields.id)
        .containsExactlyInAnyOrder(reportIdWithDefaultDefKey, reportIdWithNoDefKey);
    assertThat(alertClient.getAlertsForCollectionAsDefaultUser(collectionId))
        .extracting(AlertDefinitionDto.Fields.id)
        .containsExactly(alertIdToRemain);
    assertThat(getAllCollectionDefinitions())
        .hasSize(1)
        .extracting(CollectionDefinitionDto.Fields.data + "." + CollectionDataDto.Fields.scope)
        .contains(
            Collections.singletonList(
                new CollectionScopeEntryDto(PROCESS, DEFAULT_DEFINITION_KEY)));
    assertThat(dashboardClient.getDashboard(dashboardId).getTiles())
        .extracting(DashboardReportTileDto.Fields.id)
        .containsExactlyInAnyOrder(reportIdWithDefaultDefKey, reportIdWithNoDefKey);
    assertThat(getEventProcessPublishStateDtoFromDatabase(firstDeletingEventProcessDefinitionKey))
        .isEmpty();
    assertThat(eventInstanceIndexForPublishStateExists(publishState)).isFalse();
    assertGetMappingRequestStatusCode(
        firstDeletingEventProcessDefinitionKey, Response.Status.NOT_FOUND.getStatusCode());
    assertThat(getAllDocumentsOfVariableLabelIndex())
        .hasSize(1)
        .containsExactly(nonDeletedDefinitionVariableLabelsDto);
  }

  private EventProcessMappingDto
      createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource() {
    return eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
        Collections.singletonMap(
            USER_TASK_ID_THREE,
            createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
        "process name",
        simpleDiagramXml);
  }

  @SneakyThrows
  private List<CollectionDefinitionDto> getAllCollectionDefinitions() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        COLLECTION_INDEX_NAME, CollectionDefinitionDto.class);
  }

  private void assertGetMappingRequestStatusCode(String eventProcessMappingKey, int statusCode) {
    eventProcessClient
        .createGetEventProcessMappingRequest(eventProcessMappingKey)
        .execute(statusCode);
  }

  private void executeUpdateProcessVariableLabelRequest(
      DefinitionVariableLabelsDto labelOptimizeDto) {
    embeddedOptimizeExtension
        .getRequestExecutor()
        .buildProcessVariableLabelRequest(labelOptimizeDto)
        .execute();
  }

  private List<DefinitionVariableLabelsDto> getAllDocumentsOfVariableLabelIndex() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        VARIABLE_LABEL_INDEX_NAME, DefinitionVariableLabelsDto.class);
  }

  private DefinitionVariableLabelsDto addLabelForEventProcessDefinition(
      final String eventProcessDefinitionKey, final LabelDto labelDto) {
    DefinitionVariableLabelsDto definitionVariableLabelsDto =
        new DefinitionVariableLabelsDto(eventProcessDefinitionKey, List.of(labelDto));
    executeUpdateProcessVariableLabelRequest(definitionVariableLabelsDto);
    return definitionVariableLabelsDto;
  }
}
