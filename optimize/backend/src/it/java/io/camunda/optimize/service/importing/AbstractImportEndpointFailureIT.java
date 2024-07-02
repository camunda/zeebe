/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing;

import static io.camunda.optimize.dto.optimize.ProcessInstanceConstants.SUSPENDED_STATE;
import static io.camunda.optimize.service.db.DatabaseConstants.DECISION_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_MULTI_ALIAS;
import static io.camunda.optimize.service.db.DatabaseConstants.TENANT_INDEX_NAME;
import static io.camunda.optimize.service.util.configuration.ConfigurationServiceConstants.PLATFORM_PROFILE;
import static io.camunda.optimize.service.util.configuration.EnvironmentPropertiesConstants.INTEGRATION_TESTS;
import static io.camunda.optimize.test.it.extension.MockServerUtil.MOCKSERVER_HOST;
import static jakarta.ws.rs.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.importing.job.VariableUpdateDatabaseImportJob;
import io.camunda.optimize.test.it.extension.DatabaseIntegrationTestExtension;
import io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension;
import io.camunda.optimize.test.it.extension.EngineIntegrationExtension;
import io.camunda.optimize.test.it.extension.ErrorResponseMock;
import io.camunda.optimize.test.it.extension.IntegrationTestConfigurationUtil;
import io.camunda.optimize.test.it.extension.MockServerUtil;
import io.camunda.optimize.test.util.VariableTestUtil;
import io.github.netmikey.logunit.api.LogCapturer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@TestInstance(PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {INTEGRATION_TESTS + "=true"})
@Configuration
@Tag("import")
@ActiveProfiles(PLATFORM_PROFILE)
public abstract class AbstractImportEndpointFailureIT {

  protected static final String START_EVENT = "startEvent";
  protected static final String END_EVENT = "endEvent";
  protected static final String USER_TASK_1 = "userTask1";
  protected static final String USER_TASK_2 = "userTask2";
  protected static final String TEST_CANDIDATE_GROUP = "testCandidateGroup";
  protected static final Map<String, Object> VARIABLES =
      VariableTestUtil.createAllPrimitiveTypeVariables();

  // static extension setup with disabled cleanup to reduce initialization/cleanup overhead
  @RegisterExtension
  @Order(1)
  protected static DatabaseIntegrationTestExtension databaseIntegrationTestExtension =
      new DatabaseIntegrationTestExtension(false);

  @RegisterExtension
  @Order(2)
  protected static EngineIntegrationExtension engineIntegrationExtension =
      new EngineIntegrationExtension(false);

  @RegisterExtension
  @Order(3)
  protected static EmbeddedOptimizeExtension embeddedOptimizeExtension =
      new EmbeddedOptimizeExtension();

  @RegisterExtension
  @Order(4)
  protected final LogCapturer logCapturer =
      LogCapturer.create().captureForType(VariableUpdateDatabaseImportJob.class);

  @BeforeAll
  public void beforeAll() {
    engineIntegrationExtension.cleanEngine();
    // Due to a possible race condition with data from the previous tests not being yet in the
    // indices, we need to
    // refresh the indices before deleting the existing data
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    databaseIntegrationTestExtension.deleteAllOptimizeData();
    databaseIntegrationTestExtension.deleteAllProcessInstanceIndices();
    databaseIntegrationTestExtension.deleteAllDecisionInstanceIndices();
    embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
    // given "one of everything"
    engineIntegrationExtension.createTenant("someTenantId", "someTenantName");
    engineIntegrationExtension.deployDecisionDefinition();
    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleTwoUserTaskProcessWithVariables(VARIABLES);
    engineIntegrationExtension.createGroup(TEST_CANDIDATE_GROUP);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(TEST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.suspendProcessInstanceByInstanceId(processInstance.getId());
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("getEndpointAndErrorResponses")
  public void importWorksDespiteTemporaryFetchingFailures(
      final String endpoint, final ErrorResponseMock mockResp) {
    // given
    embeddedOptimizeExtension.resetImportStartIndexes();
    embeddedOptimizeExtension.reloadConfiguration();
    databaseIntegrationTestExtension.deleteAllOptimizeData();

    // when fetching endpoint temporarily fails
    final HttpRequest importFetcherEndpointMatcher =
        request().withPath(".*" + endpoint).withMethod(GET);
    final ClientAndServer engineMockServer =
        useAndGetMockServerForEngine(engineIntegrationExtension.getEngineName());

    mockResp.mock(importFetcherEndpointMatcher, Times.unlimited(), engineMockServer);

    // make sure fetching endpoint is called during import
    embeddedOptimizeExtension.startContinuousImportScheduling();
    Awaitility.catchUncaughtExceptions()
        .timeout(10, TimeUnit.SECONDS)
        .untilAsserted(() -> engineMockServer.verify(importFetcherEndpointMatcher));

    // endpoint no longer fails
    engineMockServer.reset();

    // wait for import to finish
    Awaitility.given()
        .ignoreExceptions()
        .timeout(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              databaseIntegrationTestExtension.refreshAllOptimizeIndices();
              assertDocumentCountInES(DECISION_DEFINITION_INDEX_NAME, 1L);
              assertDocumentCountInES(PROCESS_DEFINITION_INDEX_NAME, 1L);
              assertDocumentCountInES(TENANT_INDEX_NAME, 1L);
              assertDocumentCountInES(PROCESS_INSTANCE_MULTI_ALIAS, 1L);

              final List<ProcessInstanceDto> storedProcessInstances =
                  databaseIntegrationTestExtension.getAllProcessInstances();
              assertThat(storedProcessInstances)
                  .isNotEmpty()
                  .allSatisfy(
                      processInstanceDto -> {
                        assertThat(processInstanceDto.getUserTasks()).hasSize(2);
                        assertThat(processInstanceDto.getUserTasks())
                            .extracting(FlowNodeInstanceDto::getFlowNodeId)
                            .containsExactlyInAnyOrder(USER_TASK_1, USER_TASK_2);
                        assertThat(processInstanceDto.getUserTasks())
                            .flatExtracting(FlowNodeInstanceDto::getCandidateGroupOperations)
                            .allMatch(
                                groupOperation ->
                                    groupOperation.getGroupId().equals(TEST_CANDIDATE_GROUP));
                        assertThat(processInstanceDto.getVariables()).hasSize(VARIABLES.size());
                        assertThat(processInstanceDto.getState()).isEqualTo(SUSPENDED_STATE);
                      });
            });
  }

  private static ProcessInstanceEngineDto deployAndStartSimpleTwoUserTaskProcessWithVariables(
      final Map<String, Object> variables) {
    // @formatter:off
    final BpmnModelInstance processModel =
        Bpmn.createExecutableProcess("aProcess")
            .startEvent(START_EVENT)
            .userTask(USER_TASK_1)
            .userTask(USER_TASK_2)
            .serviceTask()
            .camundaExpression("${true}")
            .endEvent(END_EVENT)
            .done();
    // @formatter:on
    return engineIntegrationExtension.deployAndStartProcessWithVariables(processModel, variables);
  }

  protected static Stream<ErrorResponseMock> engineErrors() {
    return MockServerUtil.engineMockedErrorResponses();
  }

  private static void assertDocumentCountInES(final String elasticsearchIndex, final long count) {
    final Integer docCount =
        databaseIntegrationTestExtension.getDocumentCountOf(elasticsearchIndex);
    assertThat(docCount.longValue()).isEqualTo(count);
  }

  private ClientAndServer useAndGetMockServerForEngine(final String engineName) {
    final String mockServerUrl =
        "http://"
            + MOCKSERVER_HOST
            + ":"
            + IntegrationTestConfigurationUtil.getEngineMockServerPort()
            + "/engine-rest";
    embeddedOptimizeExtension.configureEngineRestEndpointForEngineWithName(
        engineName, mockServerUrl);
    return engineIntegrationExtension.useEngineMockServer();
  }

  protected abstract Stream<Arguments> getEndpointAndErrorResponses();
}
