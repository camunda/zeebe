/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.importing.processdefinition;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.util.BpmnModels.END_EVENT;
// import static io.camunda.optimize.util.BpmnModels.START_EVENT;
// import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockserver.model.HttpRequest.request;
//
// import io.camunda.optimize.dto.optimize.DefinitionOptimizeResponseDto;
// import io.camunda.optimize.dto.optimize.FlowNodeDataDto;
// import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.importing.AbstractImportIT;
// import jakarta.ws.rs.core.Response;
// import java.util.List;
// import org.assertj.core.groups.Tuple;
// import org.camunda.bpm.model.bpmn.BpmnModelInstance;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.mockserver.integration.ClientAndServer;
// import org.mockserver.matchers.Times;
// import org.mockserver.model.HttpResponse;
// import org.mockserver.model.MediaType;
//
// @Tag(OPENSEARCH_PASSING)
// public class ProcessDefinitionImportIT extends AbstractImportIT {
//   private static final String START = "aStart";
//   private static final String END = "anEnd";
//
//   @Test
//   public void getProcessDefinitionFields() {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram("aProcess", START,
// END));
//
//     // when
//     importAllEngineEntitiesFromScratch();
//     List<ProcessDefinitionOptimizeDto> processDefinitions =
//         databaseIntegrationTestExtension.getAllProcessDefinitions();
//
//     // then
//     assertThat(processDefinitions)
//         .singleElement()
//         .satisfies(
//             definition ->
//                 assertThat(definition.getFlowNodeData())
//                     .containsExactly(
//                         new FlowNodeDataDto(END, END, END_EVENT),
//                         new FlowNodeDataDto(START, START, START_EVENT)));
//   }
//
//   @Test
//   public void processImportedForFirstTimeMarkedAsNotOnboarded() {
//     // given
//     engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram("aProcess", START,
// END));
//
//     // when
//     importAllEngineEntitiesFromScratch();
//     List<ProcessDefinitionOptimizeDto> processDefinitions =
//         databaseIntegrationTestExtension.getAllProcessDefinitions();
//
//     // then
//     assertThat(processDefinitions)
//         .singleElement()
//         .satisfies(definition -> assertThat(definition.isOnboarded()).isFalse());
//   }
//
//   @Test
//   public void secondVersionOfProcessDefinitionImportedDoesNotOverrideOnboardedStateOfFirst() {
//     // given
//     final BpmnModelInstance process = getSimpleBpmnDiagram("aProcess", START, END);
//     engineIntegrationExtension.deployAndStartProcess(process);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//     List<ProcessDefinitionOptimizeDto> processDefinitions =
//         databaseIntegrationTestExtension.getAllProcessDefinitions();
//
//     // then
//     assertThat(processDefinitions)
//         .singleElement()
//         .satisfies(
//             definition -> {
//               assertThat(definition.getVersion()).isEqualTo("1");
//               assertThat(definition.isOnboarded()).isFalse();
//             });
//
//     engineIntegrationExtension.deployAndStartProcess(process);
//
//     // when
//     importAllEngineEntitiesFromScratch();
//     processDefinitions = databaseIntegrationTestExtension.getAllProcessDefinitions();
//
//     // then
//     assertThat(processDefinitions)
//         .hasSize(2)
//         .extracting(
//             DefinitionOptimizeResponseDto::getVersion, ProcessDefinitionOptimizeDto::isOnboarded)
//         .containsExactlyInAnyOrder(Tuple.tuple("1", false), Tuple.tuple("2", true));
//   }
//
//   @Test
//   public void deletedDefinitionsAreMarkedAsDeletedIfXmlIsUnavailable() {
//     // given
//     final ProcessInstanceEngineDto deployedProcess =
//         engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram("aProcess"));
//     final ClientAndServer engineMockServer = useAndGetEngineMockServer();
//     engineMockServer
//         .when(
//             request()
//                 .withPath(
//                     engineIntegrationExtension.getEnginePath()
//                         + "/process-definition/"
//                         + deployedProcess.getDefinitionId()
//                         + "/xml"),
//             Times.once())
//         .respond(
//             HttpResponse.response()
//                 .withStatusCode(Response.Status.NOT_FOUND.getStatusCode())
//                 .withContentType(MediaType.APPLICATION_JSON_UTF_8));
//
//     // when
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     assertThat(databaseIntegrationTestExtension.getAllProcessDefinitions())
//         .singleElement()
//         .satisfies(definition -> assertThat(definition.isDeleted()).isTrue());
//   }
// }
