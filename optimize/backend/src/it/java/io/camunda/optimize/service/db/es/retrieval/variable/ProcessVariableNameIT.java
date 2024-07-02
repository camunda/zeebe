/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.db.es.retrieval.variable;
//
// import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
// import static io.camunda.optimize.dto.optimize.ReportConstants.DEFAULT_TENANT_IDS;
// import static io.camunda.optimize.dto.optimize.ReportConstants.LATEST_VERSION;
// import static io.camunda.optimize.dto.optimize.query.variable.VariableType.STRING;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.ImmutableList;
// import com.google.common.collect.ImmutableMap;
// import com.google.common.collect.Lists;
// import io.camunda.optimize.dto.engine.definition.DecisionDefinitionEngineDto;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.variable.ProcessToQueryDto;
// import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameRequestDto;
// import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameResponseDto;
// import io.camunda.optimize.dto.optimize.query.variable.VariableType;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.rest.optimize.dto.VariableDto;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.service.util.VariableHelper;
// import io.camunda.optimize.util.BpmnModels;
// import jakarta.ws.rs.core.Response;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.IntStream;
// import org.assertj.core.groups.Tuple;
// import org.junit.jupiter.api.Test;
//
// public class ProcessVariableNameIT extends AbstractVariableIT {
//
//   @Test
//   public void getVariableNames() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", "value2");
//     variables.put("var3", "value3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.clear();
//     variables.put("var4", "value4");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(4)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var2", "var3", "var4");
//   }
//
//   @Test
//   public void getVariableNames_withQueryAgainstSubsetOfProcesses() {
//     // given
//     String key1 = "key1";
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition(key1, null);
//
//     Map<String, Object> variables1 = new HashMap<>();
//     variables1.put("p1-var1", "value1");
//     variables1.put("p1-var2", "value2");
//     variables1.put("commonVar", "someValue");
//
//     engineIntegrationExtension.startProcessInstance(processDefinition1.getId(), variables1);
//
//     String key2 = "key2";
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition(key2, null);
//
//     Map<String, Object> variables2 = new HashMap<>();
//     variables2.put("p2-var1", "value3");
//     variables2.put("p2-var2", "value4");
//     variables2.put("commonVar", "otherValue");
//
//     engineIntegrationExtension.startProcessInstance(processDefinition2.getId(), variables2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessVariableNameResponseDto> response =
//         variablesClient.getProcessVariableNames(
//             new ProcessVariableNameRequestDto(
//                 List.of(
//                     // we only query for process definition 2..
//                     new ProcessToQueryDto(
//                         key2, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS)),
//                 ProcessFilterBuilder.filter()
//                     .variable()
//                     .name("commonVar")
//                     .stringType()
//                     .operator(FilterOperator.NOT_IN)
//                     .values(List.of("otherValue")) // ..then we exclude it from the result
//                     .add()
//                     .buildList()));
//
//     // then
//     assertThat(response).extracting(ProcessVariableNameResponseDto::getName).isEmpty();
//   }
//
//   @Test
//   public void getVariableNames_withMultipleDefinitionsAndVariableExclusionFilter() {
//     // given
//     String key1 = "key1";
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition(key1, null);
//
//     Map<String, Object> variables1 = new HashMap<>();
//     variables1.put("var1", "value1");
//     variables1.put("var2", "value2");
//     variables1.put("var3", "value3");
//
//     engineIntegrationExtension.startProcessInstance(processDefinition1.getId(), variables1);
//
//     String key2 = "key2";
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition(key2, null);
//
//     Map<String, Object> variables2 = new HashMap<>();
//     variables2.put("var4", "value4");
//     variables2.put("var5", "value5");
//     variables2.put("var1", "value6");
//
//     engineIntegrationExtension.startProcessInstance(processDefinition2.getId(), variables2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessVariableNameResponseDto> response =
//         variablesClient.getProcessVariableNames(
//             new ProcessVariableNameRequestDto(
//                 List.of(
//                     new ProcessToQueryDto(
//                         key1, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS),
//                     new ProcessToQueryDto(
//                         key2, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS)),
//                 ProcessFilterBuilder.filter()
//                     .variable()
//                     .name("var3")
//                     .stringType()
//                     .operator(FilterOperator.NOT_IN)
//                     .values(List.of("value3"))
//                     .add()
//                     .buildList()));
//
//     // then
//     assertThat(response)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactlyInAnyOrderElementsOf(variables2.keySet());
//   }
//
//   @Test
//   public void getVariableNames_withMultipleDefinitionsAndNotApplicableAppliedTo() {
//     // given
//     String key1 = "key1";
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition(key1, null);
//
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", "value2");
//     variables.put("var3", "value3");
//
//     engineIntegrationExtension.startProcessInstance(processDefinition1.getId(), variables);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessVariableNameResponseDto> response =
//         variablesClient.getProcessVariableNames(
//             new ProcessVariableNameRequestDto(
//                 List.of(
//                     new ProcessToQueryDto(
//                         key1, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS)),
//                 ProcessFilterBuilder.filter()
//                     .variable()
//                     .name("var3")
//                     .stringType()
//                     .operator(FilterOperator.NOT_IN) // this should exclude the process..
//                     .values(List.of("value3"))
//                     // ..but only filters applied to "all" are considered,
//                     // so this does not get applied
//                     .appliedTo("definition")
//                     .add()
//                     .buildList()));
//
//     // then
//     assertThat(response)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactlyInAnyOrderElementsOf(variables.keySet());
//   }
//
//   @Test
//   public void getVariableNames_withMultipleDefinitionsAndCompletionFilter() {
//     // given
//     Map<String, Object> variables1 = new HashMap<>();
//     variables1.put("var1", "value1");
//     variables1.put("var2", "value2");
//     variables1.put("var3", "value3");
//
//     ProcessInstanceEngineDto completedProcessInstance =
//         engineIntegrationExtension.deployAndStartProcessWithVariables(
//             BpmnModels.getSimpleBpmnDiagram(), variables1);
//
//     Map<String, Object> variables2 = new HashMap<>();
//     variables2.put("var4", "value4");
//     variables2.put("var5", "value5");
//     variables2.put("var1", "value6");
//
//     ProcessInstanceEngineDto runningProcessInstance =
//         engineIntegrationExtension.deployAndStartProcessWithVariables(
//             BpmnModels.getSingleUserTaskDiagram(), variables2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessVariableNameResponseDto> response =
//         variablesClient.getProcessVariableNames(
//             new ProcessVariableNameRequestDto(
//                 List.of(
//                     new ProcessToQueryDto(
//                         completedProcessInstance.getProcessDefinitionKey(),
//                         Collections.singletonList(ALL_VERSIONS),
//                         DEFAULT_TENANT_IDS),
//                     new ProcessToQueryDto(
//                         runningProcessInstance.getProcessDefinitionKey(),
//                         Collections.singletonList(ALL_VERSIONS),
//                         DEFAULT_TENANT_IDS)),
//                 ProcessFilterBuilder.filter().completedInstancesOnly().add().buildList()));
//
//     // then
//     assertThat(response)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactlyInAnyOrderElementsOf(variables1.keySet());
//   }
//
//   @Test
//   public void getVariableNames_multipleDefinitions() {
//     // given
//     final String key1 = "key1";
//     final ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition(key1,
// null);
//     Map<String, Object> variables1 = new HashMap<>();
//     variables1.put("var1", "value1");
//     variables1.put("var2", "value2");
//     engineIntegrationExtension.startProcessInstance(processDefinition1.getId(), variables1);
//     final String key2 = "key2";
//     final ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition(key2,
// null);
//     Map<String, Object> variables2 = new HashMap<>();
//     // duplicate variable "var2" should not appear twice
//     variables2.put("var2", "value4");
//     variables2.put("var3", "value4");
//     variables2.put("var4", "value4");
//     engineIntegrationExtension.startProcessInstance(processDefinition2.getId(), variables2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(
//             new ProcessVariableNameRequestDto(
//                 List.of(
//                     new ProcessToQueryDto(
//                         key1, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS),
//                     new ProcessToQueryDto(
//                         key2, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS))));
//
//     // then
//     assertThat(variableResponse)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var2", "var3", "var4");
//   }
//
//   @Test
//   public void getVariableNames_multipleDefinitions_oneHasNoInstanceData() {
//     // given
//     final String key1 = "key1";
//     final ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition(key1,
// null);
//     Map<String, Object> variables1 = new HashMap<>();
//     variables1.put("var1", "value1");
//     variables1.put("var2", "value2");
//     engineIntegrationExtension.startProcessInstance(processDefinition1.getId(), variables1);
//     final String key2 = "key2";
//     deploySimpleProcessDefinition(key2, null);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(
//             new ProcessVariableNameRequestDto(
//                 List.of(
//                     new ProcessToQueryDto(
//                         key1, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS),
//                     new ProcessToQueryDto(
//                         key2, Collections.singletonList(ALL_VERSIONS), DEFAULT_TENANT_IDS))));
//
//     // then
//     assertThat(variableResponse)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var2");
//   }
//
//   @Test
//   public void getVariableNamesSingleBucketFilteredBySingleTenant() {
//     // given
//     final String tenantId1 = "tenantId1";
//     final String tenantId2 = "tenantId2";
//     final List<String> selectedTenants = Lists.newArrayList(tenantId1);
//     String processDefinition =
//         deployAndStartMultiTenantUserTaskProcess(
//             "someVariableName", Lists.newArrayList(null, tenantId1, tenantId2));
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessToQueryDto processToQuery = new ProcessToQueryDto();
//     processToQuery.setProcessDefinitionKey(processDefinition);
//     processToQuery.setProcessDefinitionVersion(ALL_VERSIONS);
//     processToQuery.setTenantIds(selectedTenants);
//
//     ProcessVariableNameRequestDto variableNameRequestDto =
//         new ProcessVariableNameRequestDto(List.of(processToQuery));
//
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(variableNameRequestDto);
//
//     // then
//     assertThat(variableResponse).hasSize(selectedTenants.size());
//   }
//
//   @Test
//   public void getVariableNamesForMultipleDefinitionVersions() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition();
//     variables.clear();
//     variables.put("var2", "value2");
//     engineIntegrationExtension.startProcessInstance(processDefinition2.getId(), variables);
//     ProcessDefinitionEngineDto processDefinition3 = deploySimpleProcessDefinition();
//     variables.clear();
//     variables.put("var3", "value3");
//     variables.put("var4", "value4");
//     startInstanceAndImportEngineEntities(processDefinition3, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(
//             processDefinition.getKey(),
//             ImmutableList.of(
//                 processDefinition.getVersionAsString(),
// processDefinition3.getVersionAsString()));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(3)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var3", "var4");
//   }
//
//   @Test
//   public void getVariableNames_worksDespiteBucketLimitExceeded() {
//     // given
//     final int bucketLimit = 2;
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getElasticSearchConfiguration()
//         .setAggregationBucketLimit(bucketLimit);
//
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", "value2");
//     variables.put("var3", "value3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.clear();
//     variables.put("var4", "value4");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(4)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var2", "var3", "var4");
//   }
//
//   @Test
//   public void getMoreThan10Variables() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     IntStream.range(0, 15).forEach(i -> variables.put("var" + i, "value" + i));
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse).hasSize(15);
//   }
//
//   @Test
//   public void getVariablesForAllVersions() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", "value2");
//     variables.put("var3", "value3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     processDefinition = deploySimpleProcessDefinition();
//     variables.clear();
//     variables.put("var4", "value4");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition.getKey(), ALL_VERSIONS);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(4)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var2", "var3", "var4");
//   }
//
//   @Test
//   public void getVariableNamesForLatestVersions() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", "value2");
//     variables.put("var3", "value3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     processDefinition = deploySimpleProcessDefinition();
//     variables.clear();
//     variables.put("var4", "value4");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition.getKey(), LATEST_VERSION);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(1)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var4");
//   }
//
//   @Test
//   public void noVariablesWithoutVersionSelection() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", "value2");
//     variables.put("var3", "value3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.clear();
//     variables.put("var4", "value4");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     ProcessToQueryDto processToQuery = new ProcessToQueryDto();
//     processToQuery.setProcessDefinitionKey(processDefinition.getKey());
//
//     ProcessVariableNameRequestDto variableRequestDto =
//         new ProcessVariableNameRequestDto(List.of(processToQuery));
//
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(variableRequestDto);
//
//     // then
//     assertThat(variableResponse).isEmpty();
//   }
//
//   @Test
//   public void noVariablesFromAnotherProcessDefinition() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.clear();
//     variables.put("var2", "value2");
//     startInstanceAndImportEngineEntities(processDefinition2, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse).hasSize(1);
//     assertThat(variableResponse.get(0).getName()).isEqualTo("var1");
//     assertThat(variableResponse.get(0).getType()).isEqualTo(STRING);
//   }
//
//   @Test
//   public void variablesAreSortedAlphabetically() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("b", "value1");
//     variables.put("c", "value2");
//     variables.put("a", "value3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.clear();
//     variables.put("c", "anotherValue");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(3)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("a", "b", "c");
//   }
//
//   @Test
//   public void variablesDoNotContainDuplicates() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(1)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1");
//   }
//
//   @Test
//   public void variableWithSameNameAndDifferentType() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var", "value1");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", true);
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(2)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var", "var");
//   }
//
//   @Test
//   public void allPrimitiveTypesCanBeRead() {
//     // given
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("dateVar", new Date());
//     variables.put("boolVar", true);
//     variables.put("shortVar", (short) 2);
//     variables.put("intVar", 5);
//     variables.put("longVar", 5L);
//     variables.put("doubleVar", 5.5);
//     variables.put("stringVar", "aString");
//
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//     embeddedOptimizeExtension.resetImportStartIndexes();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNames(processDefinition);
//
//     // then
//     assertThat(variableResponse).hasSize(variables.size());
//     for (ProcessVariableNameResponseDto responseDto : variableResponse) {
//       assertThat(variables).containsKey(responseDto.getName());
//       assertThat(VariableHelper.isProcessVariableTypeSupported(responseDto.getType())).isTrue();
//     }
//   }
//
//   @Test
//   public void objectVariablesAreExcluded() {
//     // given
//     final Map<String, Object> objectVar = new HashMap<>();
//     objectVar.put("name", "Pond");
//     final VariableDto objectVariableDto =
// variablesClient.createMapJsonObjectVariableDto(objectVar);
//     final Map<String, Object> variables = new HashMap<>();
//     variables.put("objectVar", objectVariableDto);
//
//     final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     final String reportId = createSingleReport(processDefinition);
//     importAllEngineEntitiesFromScratch();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     List<ProcessVariableNameResponseDto> variablesForDef =
//         variablesClient.getProcessVariableNames(processDefinition);
//     List<ProcessVariableNameResponseDto> variablesForReports =
//         variablesClient.getProcessVariableNamesForReportIds(Collections.singletonList(reportId));
//
//     // then only the flattened property variable is included in the result, not the raw Object
//     // variable
//     assertThat(variablesForDef)
//         .singleElement()
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactly("objectVar.name", STRING);
//     assertThat(variablesForReports)
//         .singleElement()
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactly("objectVar.name", STRING);
//   }
//
//   @Test
//   public void getVariableNamesForReport_singleReportWithVariables() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", 5L);
//     variables.put("var3", 1.5);
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//     final String reportId = createSingleReport(processDefinition);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(Collections.singletonList(reportId));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(3)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactly(
//             Tuple.tuple("var1", STRING),
//             Tuple.tuple("var2", VariableType.LONG),
//             Tuple.tuple("var3", VariableType.DOUBLE));
//   }
//
//   @Test
//   public void getVariableNamesForReport_singleReportWithMultipleDefinitions() {
//     // given
//     final String key1 = "key1";
//     final ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition(key1,
// null);
//     Map<String, Object> variables1 = new HashMap<>();
//     variables1.put("var1", "value1");
//     variables1.put("var2", "value2");
//     engineIntegrationExtension.startProcessInstance(processDefinition1.getId(), variables1);
//     final String key2 = "key2";
//     final ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition(key2,
// null);
//     Map<String, Object> variables2 = new HashMap<>();
//     // duplicate variable "var2" should not appear twice
//     variables2.put("var2", "value4");
//     variables2.put("var3", "value4");
//     variables2.put("var4", "value4");
//     engineIntegrationExtension.startProcessInstance(processDefinition2.getId(), variables2);
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(ProcessReportDataType.RAW_DATA)
//             .definitions(
//                 List.of(new ReportDataDefinitionDto(key1), new ReportDataDefinitionDto(key2)))
//             .build();
//     final String reportId = reportClient.createSingleProcessReport(reportData);
//
//     // when
//     final List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(Collections.singletonList(reportId));
//
//     // then
//     assertThat(variableResponse)
//         .extracting(ProcessVariableNameResponseDto::getName)
//         .containsExactly("var1", "var2", "var3", "var4");
//   }
//
//   @Test
//   public void getVariableNamesForReport_correctDespiteBucketLimitExceeded() {
//     // given
//     final int bucketLimit = 2;
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getElasticSearchConfiguration()
//         .setAggregationBucketLimit(bucketLimit);
//
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     variables.put("var2", 5L);
//     variables.put("var3", 1.5);
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//     final String reportId = createSingleReport(processDefinition);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(Collections.singletonList(reportId));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(3)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactly(
//             Tuple.tuple("var1", STRING),
//             Tuple.tuple("var2", VariableType.LONG),
//             Tuple.tuple("var3", VariableType.DOUBLE));
//   }
//
//   @Test
//   public void getVariableNamesForReport_reportWithNoDefinitionKey() {
//     // given
//     final String reportId = reportClient.createEmptySingleProcessReport();
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildProcessVariableNamesForReportsRequest(Collections.singletonList(reportId))
//             .executeAndReturnList(
//                 ProcessVariableNameResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(variableResponse).isEmpty();
//   }
//
//   @Test
//   public void getVariableNamesForReports_multipleReportsWithSameVariableNameAndType() {
//     // given
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition1, variables);
//     final String reportId1 = createSingleReport(processDefinition1);
//
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition2, variables);
//     final String reportId2 = createSingleReport(processDefinition1);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(Arrays.asList(reportId1, reportId2));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(1)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactly(Tuple.tuple("var1", STRING));
//   }
//
//   @Test
//   public void getVariableNamesForReports_multipleReportsWithSameVariableNameAndDifferentTypes() {
//     // given
//     Map<String, Object> variables = new HashMap<>();
//
//     variables.put("var1", "value1");
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition1, variables);
//     final String reportId1 = createSingleReport(processDefinition1);
//
//     variables.put("var1", 5L);
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition2, variables);
//     final String reportId2 = createSingleReport(processDefinition2);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(Arrays.asList(reportId1, reportId2));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(2)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactlyInAnyOrder(
//             Tuple.tuple("var1", STRING), Tuple.tuple("var1", VariableType.LONG));
//   }
//
//   @Test
//   public void getVariableNamesForReports_combinedReport() {
//     // given
//     Map<String, Object> variables = new HashMap<>();
//
//     variables.put("var1", "value1");
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition1, variables);
//     final String reportId1 = createSingleReport(processDefinition1);
//
//     variables.put("var2", 5L);
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition2, variables);
//     final String reportId2 = createSingleReport(processDefinition2);
//
//     final String combinedReportId =
//         reportClient.createCombinedReport(null, Arrays.asList(reportId1, reportId2));
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(
//             Collections.singletonList(combinedReportId));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(2)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactlyInAnyOrder(
//             Tuple.tuple("var1", STRING), Tuple.tuple("var2", VariableType.LONG));
//   }
//
//   @Test
//   public void getVariableNamesForReports_combinedReportAndSingleReport() {
//     // given
//     ProcessDefinitionEngineDto processDefinition1 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition1, ImmutableMap.of("var1", "value1"));
//     final String reportId1 = createSingleReport(processDefinition1);
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//     ProcessDefinitionEngineDto processDefinition2 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition2, ImmutableMap.of("var2", 5L));
//     final String reportId2 = createSingleReport(processDefinition2);
//     final String combinedReportId =
//         reportClient.createCombinedReport(null, Arrays.asList(reportId1, reportId2));
//
//     ProcessDefinitionEngineDto processDefinition3 = deploySimpleProcessDefinition();
//     startInstanceAndImportEngineEntities(processDefinition3, ImmutableMap.of("var3", 1.5));
//     final String reportId3 = createSingleReport(processDefinition3);
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(
//             Arrays.asList(combinedReportId, reportId3));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(3)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactlyInAnyOrder(
//             Tuple.tuple("var1", STRING),
//             Tuple.tuple("var2", VariableType.LONG),
//             Tuple.tuple("var3", VariableType.DOUBLE));
//   }
//
//   @Test
//   public void getVariableNamesForReports_decisionReportVariablesIgnored() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var1", "value1");
//     startInstanceAndImportEngineEntities(processDefinition, variables);
//     final String reportId1 = createSingleReport(processDefinition);
//
//     final DecisionDefinitionEngineDto decisionDefinitionEngineDto =
//         startDecisionInstanceAndImportEngineEntities(ImmutableMap.of("var2", 5L));
//
//     final String reportId2 =
//         reportClient
//             .createSingleDecisionReportDefinitionDto(decisionDefinitionEngineDto.getKey())
//             .getId();
//
//     // when
//     List<ProcessVariableNameResponseDto> variableResponse =
//         variablesClient.getProcessVariableNamesForReportIds(Arrays.asList(reportId1, reportId2));
//
//     // then
//     assertThat(variableResponse)
//         .hasSize(1)
//         .extracting(
//             ProcessVariableNameResponseDto::getName, ProcessVariableNameResponseDto::getType)
//         .containsExactly(Tuple.tuple("var1", STRING));
//   }
// }
