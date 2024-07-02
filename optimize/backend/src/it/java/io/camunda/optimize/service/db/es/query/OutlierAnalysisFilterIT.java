/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.query;

import static io.camunda.optimize.rest.RestTestUtil.getResponseContentAsString;
import static io.camunda.optimize.service.db.es.query.OutlierAnalysisIT.SAMPLE_OUTLIERS_HIGHER_OUTLIER_BOUND;
import static io.camunda.optimize.service.db.es.query.OutlierAnalysisIT.getBpmnModelInstance;
import static io.camunda.optimize.test.engine.OutlierDistributionClient.FLOW_NODE_ID_TEST;
import static io.camunda.optimize.test.engine.OutlierDistributionClient.VARIABLE_2_NAME;
import static io.camunda.optimize.test.engine.OutlierDistributionClient.VARIABLE_VALUE_OUTLIER;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.analysis.DurationChartEntryDto;
import io.camunda.optimize.dto.optimize.query.analysis.FindingsDto;
import io.camunda.optimize.dto.optimize.query.analysis.VariableTermDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.junit.jupiter.api.Test;

public class OutlierAnalysisFilterIT extends AbstractPlatformIT {

  @Test
  public void outlierDetection_filterByState() {
    // given
    final String testActivity1 = "testActivity1";
    final String testActivity2 = "testActivity2";
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(testActivity1, testActivity2));

    outlierDistributionClient.startPIsDistributedByDuration(
        processDefinition, new Gaussian(40 / 2., 12), 40, testActivity1, testActivity2);

    importAllEngineEntitiesFromScratch();

    // when
    Map<String, FindingsDto> outlierTest =
        analysisClient.getFlowNodeOutliers(
            processDefinition.getKey(),
            Collections.singletonList("1"),
            Collections.singletonList(null),
            runningInstanceFilterAtLevel(FilterApplicationLevel.INSTANCE));

    // then the results are filtered out by the state filter
    assertThat(outlierTest).isEmpty();
  }

  @Test
  public void outlierDetection_viewLevelFiltersNotAllowed() {
    // given
    final String testActivity1 = "testActivity1";
    final String testActivity2 = "testActivity2";
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(testActivity1, testActivity2));

    importAllEngineEntitiesFromScratch();

    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildFlowNodeOutliersRequest(
                processDefinition.getKey(),
                Collections.singletonList("1"),
                Collections.singletonList(null),
                0,
                false,
                runningInstanceFilterAtLevel(FilterApplicationLevel.VIEW))
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void durationChart_filterByState() {
    // given
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(FLOW_NODE_ID_TEST));

    for (int i = 0; i < 80; i++) {
      ProcessInstanceEngineDto processInstance =
          engineIntegrationExtension.startProcessInstance(processDefinition.getId());
      engineDatabaseExtension.changeFlowNodeTotalDuration(
          processInstance.getId(), FLOW_NODE_ID_TEST, i * 1000);
    }

    importAllEngineEntitiesFromScratch();

    // when
    List<DurationChartEntryDto> durationChart =
        analysisClient.getDurationChart(
            processDefinition.getKey(),
            Collections.singletonList("1"),
            Collections.singletonList(null),
            FLOW_NODE_ID_TEST,
            null,
            null,
            runningInstanceFilterAtLevel(FilterApplicationLevel.INSTANCE));

    // then the results are filtered out by the state filter
    assertThat(durationChart).isEmpty();
  }

  @Test
  public void durationChart_viewLevelFiltersNotAllowed() {
    // given
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(FLOW_NODE_ID_TEST));

    importAllEngineEntitiesFromScratch();

    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildFlowNodeDurationChartRequest(
                processDefinition.getKey(),
                Collections.singletonList("1"),
                FLOW_NODE_ID_TEST,
                Collections.singletonList(null),
                null,
                null,
                runningInstanceFilterAtLevel(FilterApplicationLevel.VIEW))
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void significantOutlierVariableValues_filterByState() {
    // given
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(FLOW_NODE_ID_TEST));
    outlierDistributionClient.createNormalDistributionAnd3Outliers(
        processDefinition, VARIABLE_VALUE_OUTLIER);

    importAllEngineEntitiesFromScratch();

    // when
    List<VariableTermDto> variableTermDtosActivity =
        analysisClient.getVariableTermDtos(
            SAMPLE_OUTLIERS_HIGHER_OUTLIER_BOUND,
            processDefinition.getKey(),
            Collections.singletonList("1"),
            Collections.singletonList(null),
            FLOW_NODE_ID_TEST,
            null,
            runningInstanceFilterAtLevel(FilterApplicationLevel.INSTANCE));

    // then the results are filtered out by the state filter
    assertThat(variableTermDtosActivity).isEmpty();
  }

  @Test
  public void significantOutlierVariableValues_viewLevelFilterNotAllowed() {
    // given
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(FLOW_NODE_ID_TEST));
    outlierDistributionClient.createNormalDistributionAnd3Outliers(
        processDefinition, VARIABLE_VALUE_OUTLIER);

    importAllEngineEntitiesFromScratch();

    // when
    final Response response =
        analysisClient.getVariableTermDtosActivityRawResponse(
            SAMPLE_OUTLIERS_HIGHER_OUTLIER_BOUND,
            processDefinition.getKey(),
            Collections.singletonList("1"),
            Collections.singletonList(null),
            FLOW_NODE_ID_TEST,
            null,
            runningInstanceFilterAtLevel(FilterApplicationLevel.VIEW));

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void significantOutlierVariableValuesProcessInstanceIdExport_filterByState() {
    // given
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(FLOW_NODE_ID_TEST));
    outlierDistributionClient.createNormalDistributionAnd3Outliers(
        processDefinition, VARIABLE_VALUE_OUTLIER);
    importAllEngineEntitiesFromScratch();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildSignificantOutlierVariableTermsInstanceIdsRequest(
                processDefinition.getKey(),
                Collections.singletonList("1"),
                Collections.singletonList(null),
                FLOW_NODE_ID_TEST,
                null,
                SAMPLE_OUTLIERS_HIGHER_OUTLIER_BOUND,
                VARIABLE_2_NAME,
                VARIABLE_VALUE_OUTLIER,
                runningInstanceFilterAtLevel(FilterApplicationLevel.INSTANCE))
            .execute();

    // then the response contains no instances, as the state filter excludes them
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    final String csvContent = getResponseContentAsString(response);
    final String[] lines = csvContent.split("\\r\\n");
    assertThat(lines).hasSize(1);
    assertThat(lines[0]).isEqualTo("\"processInstanceId\"");
  }

  @Test
  public void significantOutlierVariableValuesProcessInstanceIdExport_viewLevelFilterNotAllowed() {
    // given
    ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getBpmnModelInstance(FLOW_NODE_ID_TEST));
    importAllEngineEntitiesFromScratch();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildSignificantOutlierVariableTermsInstanceIdsRequest(
                processDefinition.getKey(),
                Collections.singletonList("1"),
                Collections.singletonList(null),
                FLOW_NODE_ID_TEST,
                null,
                SAMPLE_OUTLIERS_HIGHER_OUTLIER_BOUND,
                VARIABLE_2_NAME,
                VARIABLE_VALUE_OUTLIER,
                runningInstanceFilterAtLevel(FilterApplicationLevel.VIEW))
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  private static List<ProcessFilterDto<?>> runningInstanceFilterAtLevel(
      FilterApplicationLevel filterApplicationLevel) {
    return ProcessFilterBuilder.filter()
        .runningInstancesOnly()
        .filterLevel(filterApplicationLevel)
        .add()
        .buildList();
  }
}
