/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.process;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_PROCESS_DEFINITION;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_USER;
import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static io.camunda.optimize.test.engine.AuthorizationClient.SPIDERMAN_USER;
import static io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.DefinitionOptimizeResponseDto;
import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.datasource.EngineDataSourceDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestRequestDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestResponseDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewResponseDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessUpdateDto;
import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import io.camunda.optimize.dto.optimize.rest.sorting.ProcessOverviewSorter;
import io.camunda.optimize.service.db.es.schema.index.ProcessDefinitionIndexES;
import io.camunda.optimize.service.util.IdGenerator;
import io.camunda.optimize.util.BpmnModels;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class ProcessOverviewRetrievalIT extends AbstractPlatformIT {

  private final String FIRST_PROCESS_DEFINITION_KEY = "firstDefKey";
  private final String SECOND_PROCESS_DEFINITION_KEY = "secondDefKey";

  @Test
  public void getProcessOverview_notPossibleForUnauthenticatedUser() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetProcessOverviewRequest(null)
            .withoutAuthentication()
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void getProcessOverview_noProcessDefinitionFound() {
    // when
    final List<ProcessOverviewResponseDto> processes =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetProcessOverviewRequest(null)
            .executeAndReturnList(
                ProcessOverviewResponseDto.class, Response.Status.OK.getStatusCode());

    // then
    assertThat(processes).isEmpty();
  }

  @Test
  public void getProcessOverview_userCanOnlySeeAuthorizedProcesses() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deploySimpleProcessDefinition(FIRST_PROCESS_DEFINITION_KEY);
    deploySimpleProcessDefinition(SECOND_PROCESS_DEFINITION_KEY);
    importAllEngineEntitiesFromScratch();

    // when
    List<ProcessOverviewResponseDto> processes =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetProcessOverviewRequest(null)
            .withUserAuthentication(KERMIT_USER, KERMIT_USER)
            .executeAndReturnList(
                ProcessOverviewResponseDto.class, Response.Status.OK.getStatusCode());

    // then
    assertThat(processes).isEmpty();
  }

  @Test
  public void getProcessOverview_eventBasedProcessedNotShownOnProcessOverview() {
    // given
    databaseIntegrationTestExtension.addEventProcessDefinitionDtoToDatabase(
        SECOND_PROCESS_DEFINITION_KEY, new IdentityDto(DEFAULT_USERNAME, IdentityType.USER));
    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes).isEmpty();
  }

  @Test
  public void getProcessOverview_fetchedAccordingToAscendingOrderWhenSortOrderIsNull() {
    // given
    deploySimpleProcessDefinition(FIRST_PROCESS_DEFINITION_KEY);
    deploySimpleProcessDefinition(SECOND_PROCESS_DEFINITION_KEY);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessOverviewSorter sorter =
        new ProcessOverviewSorter(ProcessOverviewResponseDto.Fields.processDefinitionName, null);
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(sorter);

    // then sort in ascending order
    assertThat(processes)
        .hasSize(2)
        .isSortedAccordingTo(
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName));
  }

  @ParameterizedTest
  @MethodSource("getSortCriteriaAndExpectedComparator")
  public void getProcessOverview_sortByValidSortFields(
      final String sortBy,
      final SortOrder sortingOrder,
      final Comparator<ProcessOverviewResponseDto> comparator) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.grantAllResourceAuthorizationsForKermit(RESOURCE_TYPE_PROCESS_DEFINITION);
    final ProcessDefinitionEngineDto firstDef =
        deploySimpleProcessDefinition(FIRST_PROCESS_DEFINITION_KEY);
    final ProcessDefinitionEngineDto secondDef =
        deploySimpleProcessDefinition(SECOND_PROCESS_DEFINITION_KEY);
    importAllEngineEntitiesFromScratch();
    setProcessOwner(firstDef.getKey(), DEFAULT_USERNAME);
    setProcessOwner(secondDef.getKey(), KERMIT_USER);

    // when
    ProcessOverviewSorter sorter = new ProcessOverviewSorter(sortBy, sortingOrder);
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(sorter);

    // then
    assertThat(processes).hasSize(2).isSortedAccordingTo(comparator);
  }

  @ParameterizedTest
  @MethodSource("getSortOrderAndExpectedDefinitionKeyComparator")
  public void getProcessOverview_sortByKeyWhenNamesAreIdentical(
      final SortOrder sortOrder, final Comparator<ProcessOverviewResponseDto> comparator) {
    // given
    addProcessDefinitionWithGivenNameAndKeyToElasticSearch("sameName", "a");
    addProcessDefinitionWithGivenNameAndKeyToElasticSearch("sameName", "b");
    addProcessDefinitionWithGivenNameAndKeyToElasticSearch("sameName", "c");
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    importAllEngineEntitiesFromScratch();

    // when
    ProcessOverviewSorter sorter =
        new ProcessOverviewSorter(
            ProcessOverviewResponseDto.Fields.processDefinitionName, sortOrder);
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(sorter);

    // then
    assertThat(processes).hasSize(3).isSortedAccordingTo(comparator);
  }

  @Test
  public void getProcessOverview_sortByProcessNameWhenProcessHasNoName() {
    // given
    final String firstProcessName = "aProcessName";
    addProcessDefinitionWithGivenNameAndKeyToElasticSearch(firstProcessName, "a");
    addProcessDefinitionWithGivenNameAndKeyToElasticSearch(null, "b");
    final String thirdProcessName = "cProcessName";
    addProcessDefinitionWithGivenNameAndKeyToElasticSearch(thirdProcessName, "c");
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    importAllEngineEntitiesFromScratch();

    // when
    ProcessOverviewSorter sorter =
        new ProcessOverviewSorter(
            ProcessOverviewResponseDto.Fields.processDefinitionName, SortOrder.ASC);
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(sorter);

    // then
    assertThat(processes)
        .hasSize(3)
        .extracting(ProcessOverviewResponseDto::getProcessDefinitionName)
        .containsExactly(firstProcessName, "b", thirdProcessName);
  }

  @ParameterizedTest
  @MethodSource("getDefinitionNameAndExpectedSortOrder")
  public void
      getProcessOverview_processesGetOrderedByOwnerAndDefinitionNameWhenOwnerNameIsMissingFromSomeDefinitions(
          final SortOrder sortOrder, final List<String> processDefinitionKeys) {
    // given
    authorizationClient.addSpidermanUserAndGrantAccessToOptimize();
    processDefinitionKeys.forEach(this::deploySimpleProcessDefinition);
    importAllEngineEntitiesFromScratch();
    setProcessOwner("firstDefWithOwner", DEFAULT_USERNAME);
    setProcessOwner("secondDefWithOwner", SPIDERMAN_USER);
    importAllEngineEntitiesFromLastIndex();

    // when
    ProcessOverviewSorter sorter =
        new ProcessOverviewSorter(ProcessOverviewResponseDto.Fields.owner, sortOrder);
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(sorter);

    // then
    assertThat(processes)
        .hasSize(4)
        .extracting(ProcessOverviewResponseDto::getProcessDefinitionKey)
        .isEqualTo(processDefinitionKeys);
  }

  @ParameterizedTest
  @MethodSource("getSortOrderAndExpectedDefinitionNameComparator")
  public void getProcessOverview_processesOrderedByProcessDefinitionNameWhenTheyHaveSameOwner(
      final SortOrder sortOrder, final Comparator<ProcessOverviewResponseDto> comparator) {
    // given
    deploySimpleProcessDefinition("a");
    deploySimpleProcessDefinition("b");
    importAllEngineEntitiesFromScratch();
    setProcessOwner("a", DEFAULT_USERNAME);
    setProcessOwner("b", DEFAULT_USERNAME);

    // when
    ProcessOverviewSorter sorter =
        new ProcessOverviewSorter(ProcessOverviewResponseDto.Fields.owner, sortOrder);
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(sorter);

    // then
    assertThat(processes).hasSize(2).isSortedAccordingTo(comparator);
  }

  @Test
  public void getProcessOverview_invalidSortParameter() {
    // given
    ProcessOverviewSorter sorter = new ProcessOverviewSorter("invalid", SortOrder.ASC);

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetProcessOverviewRequest(sorter)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void getProcessDigests() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.grantSingleResourceAuthorizationsForUser(
        DEFAULT_USERNAME, "kermit", RESOURCE_TYPE_USER);
    engineIntegrationExtension.deployAndStartProcess(
        getSimpleBpmnDiagram(FIRST_PROCESS_DEFINITION_KEY));
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram("anotherProcess"));
    importAllEngineEntitiesFromScratch();
    processOverviewClient.updateProcess(
        FIRST_PROCESS_DEFINITION_KEY, DEFAULT_USERNAME, new ProcessDigestRequestDto(false));
    processOverviewClient.updateProcess(
        "anotherProcess", "kermit", new ProcessDigestRequestDto(false));

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    assertThat(processes)
        .hasSize(2)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getDigest)
        .containsExactlyInAnyOrder(
            Tuple.tuple(FIRST_PROCESS_DEFINITION_KEY, new ProcessDigestResponseDto(false)),
            Tuple.tuple("anotherProcess", new ProcessDigestResponseDto(false)));
  }

  @Test
  public void getProcessOverviewWhenProcessHasChangedName() {
    // given
    final String originalName = "originalName";
    engineIntegrationExtension.deployAndStartProcess(
        createSimpleProcessWithKeyAndName(FIRST_PROCESS_DEFINITION_KEY, originalName));
    importAllEngineEntitiesFromScratch();

    // then
    assertThat(processOverviewClient.getProcessOverviews())
        .hasSize(1)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getProcessDefinitionName)
        .containsExactly(Tuple.tuple(FIRST_PROCESS_DEFINITION_KEY, originalName));

    // when
    final String updatedName = "updatedName";
    engineIntegrationExtension.deployAndStartProcess(
        createSimpleProcessWithKeyAndName(FIRST_PROCESS_DEFINITION_KEY, updatedName));
    importAllEngineEntitiesFromLastIndex();
    // We have to invalidate the cache to make sure that the first definition is not the one
    // returned on fetching overviews
    embeddedOptimizeExtension.reloadConfiguration();

    // then
    assertThat(processOverviewClient.getProcessOverviews())
        .hasSize(1)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getProcessDefinitionName)
        .containsExactly(Tuple.tuple(FIRST_PROCESS_DEFINITION_KEY, updatedName));
  }

  private BpmnModelInstance createSimpleProcessWithKeyAndName(final String key, final String name) {
    return Bpmn.createExecutableProcess(key)
        .name(name)
        .startEvent(BpmnModels.START_EVENT_ID)
        .endEvent(BpmnModels.END_EVENT_ID_1)
        .done();
  }

  private ProcessDefinitionOptimizeDto createProcessDefinition(String definitionKey, String name) {
    return ProcessDefinitionOptimizeDto.builder()
        .id(IdGenerator.getNextId())
        .key(definitionKey)
        .name(name)
        .version("1")
        .dataSource(new EngineDataSourceDto(DEFAULT_ENGINE_ALIAS))
        .bpmn20Xml("xml")
        .build();
  }

  private void addProcessDefinitionWithGivenNameAndKeyToElasticSearch(String name, String key) {
    final DefinitionOptimizeResponseDto definition = createProcessDefinition(key, name);
    databaseIntegrationTestExtension.addEntriesToDatabase(
        new ProcessDefinitionIndexES().getIndexName(), Map.of(definition.getId(), definition));
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
  }

  private void setProcessOwner(final String processDefKey, final String ownerId) {
    embeddedOptimizeExtension
        .getRequestExecutor()
        .buildUpdateProcessRequest(
            processDefKey, new ProcessUpdateDto(ownerId, new ProcessDigestRequestDto()))
        .execute();
  }

  private ProcessDefinitionEngineDto deploySimpleProcessDefinition(String processDefinitionKey) {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getSimpleBpmnDiagram(processDefinitionKey));
  }

  private static Stream<Arguments> getSortOrderAndExpectedDefinitionNameComparator() {
    return Stream.of(
        Arguments.of(
            SortOrder.ASC,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName)),
        Arguments.of(
            null, Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName)),
        Arguments.of(
            SortOrder.DESC,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName).reversed()));
  }

  private static Stream<Arguments> getDefinitionNameAndExpectedSortOrder() {
    return Stream.of(
        Arguments.of(
            SortOrder.ASC,
            List.of(
                "secondDefWithOwner",
                "firstDefWithOwner",
                "fourthDefWithOwner",
                "thirdDefWithOwner")),
        Arguments.of(
            null,
            List.of(
                "secondDefWithOwner",
                "firstDefWithOwner",
                "fourthDefWithOwner",
                "thirdDefWithOwner")),
        Arguments.of(
            SortOrder.DESC,
            List.of(
                "thirdDefWithOwner",
                "fourthDefWithOwner",
                "firstDefWithOwner",
                "secondDefWithOwner")));
  }

  private static Stream<Arguments> getSortCriteriaAndExpectedComparator() {
    return Stream.of(
        Arguments.of(
            ProcessOverviewResponseDto.Fields.processDefinitionName,
            SortOrder.ASC,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName)),
        Arguments.of(
            ProcessOverviewResponseDto.Fields.processDefinitionName,
            null,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName)),
        Arguments.of(
            ProcessOverviewResponseDto.Fields.processDefinitionName,
            SortOrder.DESC,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionName).reversed()),
        Arguments.of(
            ProcessOverviewResponseDto.Fields.owner,
            SortOrder.ASC,
            Comparator.comparing(
                (ProcessOverviewResponseDto processOverviewResponseDto) ->
                    processOverviewResponseDto.getOwner().getName(),
                Comparator.nullsLast(Comparator.naturalOrder()))),
        Arguments.of(
            ProcessOverviewResponseDto.Fields.owner,
            null,
            Comparator.comparing(
                (ProcessOverviewResponseDto processOverviewResponseDto) ->
                    processOverviewResponseDto.getOwner().getName(),
                Comparator.nullsLast(Comparator.naturalOrder()))),
        Arguments.of(
            ProcessOverviewResponseDto.Fields.owner,
            SortOrder.DESC,
            Comparator.comparing(
                    (ProcessOverviewResponseDto processOverviewResponseDto) ->
                        processOverviewResponseDto.getOwner().getName(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()));
  }

  private static Stream<Arguments> getSortOrderAndExpectedDefinitionKeyComparator() {
    return Stream.of(
        Arguments.of(
            SortOrder.ASC,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionKey)),
        Arguments.of(
            null, Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionKey)),
        Arguments.of(
            SortOrder.DESC,
            Comparator.comparing(ProcessOverviewResponseDto::getProcessDefinitionKey).reversed()));
  }
}
