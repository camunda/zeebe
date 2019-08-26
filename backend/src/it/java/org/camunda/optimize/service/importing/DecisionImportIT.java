/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.engine.DecisionDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.importing.DecisionInstanceDto;
import org.camunda.optimize.dto.optimize.importing.index.AllEntitiesBasedImportIndexDto;
import org.camunda.optimize.dto.optimize.importing.index.TimestampBasedImportIndexDto;
import org.camunda.optimize.service.es.schema.index.DecisionDefinitionIndex;
import org.camunda.optimize.service.es.schema.index.DecisionInstanceIndex;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

import static org.camunda.optimize.service.es.schema.index.index.TimestampBasedImportIndex.ES_TYPE_INDEX_REFERS_TO;
import static org.camunda.optimize.service.es.schema.index.index.TimestampBasedImportIndex.TIMESTAMP_BASED_IMPORT_INDEX_TYPE;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DECISION_DEFINITION_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DECISION_INSTANCE_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.IMPORT_INDEX_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.PROCESS_DEFINITION_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.PROCESS_INSTANCE_INDEX_NAME;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

public class DecisionImportIT extends AbstractImportIT {

  protected static final Set<String> DECISION_DEFINITION_NULLABLE_FIELDS =
    Collections.singleton(DecisionDefinitionIndex.TENANT_ID);

  @Test
  public void importOfDecisionDataCanBeDisabled() throws IOException {
    // given
    embeddedOptimizeRule.getConfigurationService().setImportDmnDataEnabled(false);
    embeddedOptimizeRule.reloadConfiguration();
    engineRule.deployAndStartDecisionDefinition();
    BpmnModelInstance exampleProcess = Bpmn.createExecutableProcess().name("foo").startEvent().endEvent().done();
    engineRule.deployAndStartProcess(exampleProcess);

    // when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // then
    allEntriesInElasticsearchHaveAllDataWithCount(DECISION_DEFINITION_INDEX_NAME, 0L);
    allEntriesInElasticsearchHaveAllDataWithCount(DECISION_INSTANCE_INDEX_NAME, 0L);
    allEntriesInElasticsearchHaveAllDataWithCount(PROCESS_INSTANCE_INDEX_NAME, 1L);
    allEntriesInElasticsearchHaveAllDataWithCount(PROCESS_DEFINITION_INDEX_NAME, 1L);
  }

  @Test
  public void allDecisionDefinitionFieldDataIsAvailable() throws IOException {
    //given
    engineRule.deployDecisionDefinition();
    engineRule.deployDecisionDefinition();

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    allEntriesInElasticsearchHaveAllDataWithCount(DECISION_DEFINITION_INDEX_NAME, 2L, DECISION_DEFINITION_NULLABLE_FIELDS);
  }

  @Test
  public void decisionDefinitionTenantIdIsImportedIfPresent() throws IOException {
    //given
    final String tenantId = "reallyAwesomeTenantId";
    engineRule.deployDecisionDefinitionWithTenant(tenantId);

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_DEFINITION_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));
    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertThat(hit.getSourceAsMap().get(DecisionDefinitionIndex.TENANT_ID), is(tenantId));
  }

  @Test
  public void decisionDefinitionDefaultEngineTenantIdIsApplied() throws IOException {
    //given
    final String tenantId = "reallyAwesomeTenantId";
    embeddedOptimizeRule.getDefaultEngineConfiguration().getDefaultTenant().setId(tenantId);
    engineRule.deployDecisionDefinition();

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_DEFINITION_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));
    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertThat(hit.getSourceAsMap().get(DecisionDefinitionIndex.TENANT_ID), is(tenantId));
  }

  @Test
  public void decisionDefinitionEngineTenantIdIsPreferredOverDefaultTenantId() throws IOException {
    //given
    final String defaultTenantId = "reallyAwesomeTenantId";
    final String expectedTenantId = "evenMoreAwesomeTenantId";
    embeddedOptimizeRule.getDefaultEngineConfiguration().getDefaultTenant().setId(defaultTenantId);
    engineRule.deployDecisionDefinitionWithTenant(expectedTenantId);

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_DEFINITION_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));
    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertThat(hit.getSourceAsMap().get(DecisionDefinitionIndex.TENANT_ID), is(expectedTenantId));
  }

  @Test
  public void decisionInstanceFieldDataIsAvailable() throws IOException {
    //given
    engineRule.deployAndStartDecisionDefinition();

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));

    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertDecisionInstanceFieldSetAsExpected(hit, false);
  }

  @Test
  public void decisionInstanceTenantIdIsImportedIfPresent() throws IOException {
    //given
    final String tenantId = "reallyAwesomeTenantId";
    final DecisionDefinitionEngineDto decisionDefinitionDto = engineRule.deployDecisionDefinitionWithTenant(tenantId);
    engineRule.startDecisionInstance(decisionDefinitionDto.getId());

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));
    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertThat(hit.getSourceAsMap().get(DecisionInstanceIndex.TENANT_ID), is(tenantId));
  }

  @Test
  public void decisionInstanceDefaultEngineTenantIdIsApplied() throws IOException {
    //given
    final String tenantId = "reallyAwesomeTenantId";
    embeddedOptimizeRule.getDefaultEngineConfiguration().getDefaultTenant().setId(tenantId);
    engineRule.deployAndStartDecisionDefinition();

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));
    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertThat(hit.getSourceAsMap().get(DecisionInstanceIndex.TENANT_ID), is(tenantId));
  }

  @Test
  public void decisionInstanceEngineTenantIdIsPreferredOverDefaultTenantId() throws IOException {
    //given
    final String defaultTenantId = "reallyAwesomeTenantId";
    final String expectedTenantId = "evenMoreAwesomeTenantId";
    embeddedOptimizeRule.getDefaultEngineConfiguration().getDefaultTenant().setId(defaultTenantId);
    final DecisionDefinitionEngineDto decisionDefinitionDto = engineRule.deployDecisionDefinitionWithTenant(
      expectedTenantId);
    engineRule.startDecisionInstance(decisionDefinitionDto.getId());

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(DECISION_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits(), is(1L));
    final SearchHit hit = idsResp.getHits().getHits()[0];
    assertThat(hit.getSourceAsMap().get(DecisionInstanceIndex.TENANT_ID), is(expectedTenantId));
  }

  @Test
  public void multipleDecisionInstancesAreImported() throws IOException {
    //given
    DecisionDefinitionEngineDto decisionDefinitionEngineDto = engineRule.deployAndStartDecisionDefinition();
    engineRule.startDecisionInstance(decisionDefinitionEngineDto.getId());

    //when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //then
    allEntriesInElasticsearchHaveAllDataWithCount(DECISION_INSTANCE_INDEX_NAME, 2L);
  }

  @Test
  public void decisionImportIndexesAreStored() throws IOException {
    // given
    engineRule.deployAndStartDecisionDefinition();
    engineRule.deployAndStartDecisionDefinition();
    engineRule.deployAndStartDecisionDefinition();

    // when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    embeddedOptimizeRule.storeImportIndexesToElasticsearch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // then
    SearchResponse searchDecisionInstanceTimestampBasedIndexResponse = getDecisionInstanceIndexResponse();
    assertThat(searchDecisionInstanceTimestampBasedIndexResponse.getHits().getTotalHits(), is(1L));
    final TimestampBasedImportIndexDto decisionInstanceDto = parseToDto(
      searchDecisionInstanceTimestampBasedIndexResponse.getHits().getHits()[0], TimestampBasedImportIndexDto.class
    );
    assertThat(decisionInstanceDto.getTimestampOfLastEntity(), is(lessThan(OffsetDateTime.now())));

    final String decisionDefinitionIndexId = DECISION_DEFINITION_INDEX_NAME + "-1";
    SearchResponse searchDecisionDefinitionIndexResponse = getDecisionDefinitionIndexById(decisionDefinitionIndexId);
    assertThat(searchDecisionDefinitionIndexResponse.getHits().getTotalHits(), is(1L));
    final AllEntitiesBasedImportIndexDto definitionImportIndex = parseToDto(
      searchDecisionDefinitionIndexResponse.getHits().getHits()[0],
      AllEntitiesBasedImportIndexDto.class
    );
    assertThat(definitionImportIndex.getImportIndex(), is(3L));
  }

  @Test
  public void importMoreThanOnePage() throws Exception {
    // given
    int originalMaxPageSize = embeddedOptimizeRule.getConfigurationService()
      .getEngineImportProcessInstanceMaxPageSize();
    embeddedOptimizeRule.getConfigurationService().setEngineImportDecisionInstanceMaxPageSize(1);
    engineRule.deployAndStartDecisionDefinition();
    engineRule.deployAndStartDecisionDefinition();

    // when
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    embeddedOptimizeRule.importAllEngineEntitiesFromLastIndex();
    elasticSearchRule.refreshAllOptimizeIndices();

    // then
    allEntriesInElasticsearchHaveAllDataWithCount(DECISION_INSTANCE_INDEX_NAME, 2L);
    embeddedOptimizeRule.getConfigurationService().setEngineImportDecisionInstanceMaxPageSize(originalMaxPageSize);
  }

  private SearchResponse getDecisionDefinitionIndexById(final String decisionDefinitionIndexId) throws IOException {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(termsQuery("_id", decisionDefinitionIndexId))
      .size(100);

    SearchRequest searchRequest = new SearchRequest()
      .indices(IMPORT_INDEX_INDEX_NAME)
      .types(IMPORT_INDEX_INDEX_NAME)
      .source(searchSourceBuilder);

    return elasticSearchRule.getOptimizeElasticClient().search(searchRequest, RequestOptions.DEFAULT);
  }

  private SearchResponse getDecisionInstanceIndexResponse() throws IOException {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(termsQuery(ES_TYPE_INDEX_REFERS_TO, DECISION_INSTANCE_INDEX_NAME))
      .size(100);

    SearchRequest searchRequest = new SearchRequest()
      .indices(TIMESTAMP_BASED_IMPORT_INDEX_TYPE)
      .types(TIMESTAMP_BASED_IMPORT_INDEX_TYPE)
      .source(searchSourceBuilder);

    return elasticSearchRule.getOptimizeElasticClient().search(searchRequest, RequestOptions.DEFAULT);
  }


  private <T> T parseToDto(final SearchHit searchHit, Class<T> dtoClass) {
    try {
      return elasticSearchRule.getObjectMapper().readValue(searchHit.getSourceAsString(), dtoClass);
    } catch (IOException e) {
      throw new RuntimeException("Failed parsing dto: " + dtoClass.getSimpleName());
    }
  }

  @Override
  protected void allEntriesInElasticsearchHaveAllDataWithCount(final String elasticsearchIndex,
                                                               final long count) throws IOException {
    allEntriesInElasticsearchHaveAllDataWithCount(elasticsearchIndex, count, false);
  }

  private void allEntriesInElasticsearchHaveAllDataWithCount(final String elasticsearchIndex,
                                                             final long count,
                                                             final boolean expectTenant) throws IOException {
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(elasticsearchIndex);

    assertThat(idsResp.getHits().getTotalHits(), is(count));
    for (SearchHit searchHit : idsResp.getHits().getHits()) {
      // in this test suite we only care about decision types, no asserts besides count on others
      if (DECISION_INSTANCE_INDEX_NAME.equals(elasticsearchIndex)) {
        assertDecisionInstanceFieldSetAsExpected(searchHit, expectTenant);
      } else if (DECISION_DEFINITION_INDEX_NAME.equals(elasticsearchIndex)) {
        assertAllFieldsSet(DECISION_DEFINITION_NULLABLE_FIELDS, searchHit);
      }
    }
  }

  private void assertDecisionInstanceFieldSetAsExpected(final SearchHit hit, final boolean expectTenant) {
    final DecisionInstanceDto dto = parseToDto(hit, DecisionInstanceDto.class);
    assertThat(dto.getProcessDefinitionId(), is(nullValue()));
    assertThat(dto.getProcessDefinitionKey(), is(nullValue()));
    assertThat(dto.getDecisionDefinitionId(), is(notNullValue()));
    assertThat(dto.getDecisionDefinitionKey(), is(notNullValue()));
    assertThat(dto.getDecisionDefinitionVersion(), is(notNullValue()));
    assertThat(dto.getEvaluationDateTime(), is(notNullValue()));
    assertThat(dto.getProcessInstanceId(), is(nullValue()));
    assertThat(dto.getRootProcessInstanceId(), is(nullValue()));
    assertThat(dto.getActivityId(), is(nullValue()));
    assertThat(dto.getCollectResultValue(), is(nullValue()));
    assertThat(dto.getRootDecisionInstanceId(), is(nullValue()));
    assertThat(dto.getInputs().size(), is(2));
    dto.getInputs().forEach(inputInstanceDto -> {
      assertThat(inputInstanceDto.getId(), is(notNullValue()));
      assertThat(inputInstanceDto.getClauseId(), is(notNullValue()));
      assertThat(inputInstanceDto.getClauseName(), is(notNullValue()));
      assertThat(inputInstanceDto.getType(), is(notNullValue()));
      assertThat(inputInstanceDto.getValue(), is(notNullValue()));
    });
    assertThat(dto.getOutputs().size(), is(2));
    dto.getOutputs().forEach(outputInstanceDto -> {
      assertThat(outputInstanceDto.getId(), is(notNullValue()));
      assertThat(outputInstanceDto.getClauseId(), is(notNullValue()));
      assertThat(outputInstanceDto.getClauseName(), is(notNullValue()));
      assertThat(outputInstanceDto.getType(), is(notNullValue()));
      assertThat(outputInstanceDto.getValue(), is(notNullValue()));
      assertThat(outputInstanceDto.getRuleId(), is(notNullValue()));
      assertThat(outputInstanceDto.getRuleOrder(), is(notNullValue()));
    });
    assertThat(dto.getEngine(), is(notNullValue()));
    assertThat(dto.getTenantId(), is(expectTenant ? notNullValue() : nullValue()));
  }

}
