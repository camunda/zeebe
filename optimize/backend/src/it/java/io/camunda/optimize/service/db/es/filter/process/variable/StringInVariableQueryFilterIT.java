/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.db.es.filter.process.variable;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator.IN;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator.NOT_IN;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.ImmutableMap;
// import com.google.common.collect.Lists;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.service.db.es.filter.process.AbstractFilterIT;
// import io.camunda.optimize.test.it.extension.EngineVariableValue;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Stream;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
//
// @Tag(OPENSEARCH_PASSING)
// public class StringInVariableQueryFilterIT extends AbstractFilterIT {
//
//   private static final String STRING_VARIABLE_NAME = "stringVar";
//
//   @Test
//   public void simpleStringVariableFilter() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var", "value");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", "anotherValue");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .name("var")
//             .stringType()
//             .values(Collections.singletonList("value"))
//             .operator(IN)
//             .add()
//             .buildList();
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(1);
//   }
//
//   @Test
//   public void severalVariablesInSameProcessInstanceShouldNotAffectFilter() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put(STRING_VARIABLE_NAME, "aStringValue");
//     variables.put("anotherStringVar", "anotherValue");
//     variables.put("boolVar", true);
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .name(STRING_VARIABLE_NAME)
//             .stringType()
//             .values(Collections.singletonList("aStringValue"))
//             .operator(NOT_IN)
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).isEmpty();
//   }
//
//   @Test
//   public void variablesWithDifferentNameAreFiltered() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put(STRING_VARIABLE_NAME, "value");
//     variables.put("anotherStringVar", "value");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .stringType()
//             .name(STRING_VARIABLE_NAME)
//             .operator(IN)
//             .values(Collections.singletonList("value"))
//             .add()
//             .buildList();
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(1);
//   }
//
//   @Test
//   public void variablesWithDifferentTypeAreFiltered() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var", "1");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", 1);
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .name("var")
//             .stringType()
//             .values(Collections.singletonList("1"))
//             .operator(IN)
//             .add()
//             .buildList();
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(1);
//   }
//
//   @Test
//   public void stringEqualityFilterWithVariableOfDifferentType() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put(STRING_VARIABLE_NAME, "aStringValue");
//     variables.put("anotherStringVar", "anotherValue");
//     variables.put("boolVar", true);
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .stringType()
//             .name(STRING_VARIABLE_NAME)
//             .values(Collections.singletonList("aStringValue"))
//             .operator(IN)
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(1);
//   }
//
//   @Test
//   public void stringInequalityFilterWithVariableOfDifferentTypeAndProcessInstance() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put(STRING_VARIABLE_NAME, "aStringValue");
//     variables.put("boolVar", true);
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("anotherStringVar", "aStringValue");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .stringType()
//             .name("anotherStringVar")
//             .operator(NOT_IN)
//             .values(Collections.singletonList("aStringValue"))
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(2);
//   }
//
//   @Test
//   public void severalStringValueFiltersAreConcatenated() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     Map<String, Object> variables = new HashMap<>();
//     variables.put(STRING_VARIABLE_NAME, "aStringValue");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put(STRING_VARIABLE_NAME, "anotherValue");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<String> values = new ArrayList<>();
//     values.add("aStringValue");
//     values.add("anotherValue");
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .stringType()
//             .name(STRING_VARIABLE_NAME)
//             .operator(IN)
//             .values(values)
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(2);
//   }
//
//   private static Stream<Arguments> nullFilterScenarios() {
//     return Stream.of(
//         Arguments.of(IN, Collections.singletonList(null), 2),
//         Arguments.of(IN, Lists.newArrayList(null, "validMatch"), 3),
//         Arguments.of(NOT_IN, Collections.singletonList(null), 2),
//         Arguments.of(NOT_IN, Lists.newArrayList(null, "validMatch"), 1));
//   }
//
//   @ParameterizedTest
//   @MethodSource("nullFilterScenarios")
//   public void stringFilterSupportsNullValue(
//       final FilterOperator operator,
//       final List<String> filterValues,
//       final Integer expectedInstanceCount) {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     // instance where the variable is undefined
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // instance where the variable has the value null
//     engineIntegrationExtension.startProcessInstance(
//         processDefinition.getId(),
//         Collections.singletonMap(STRING_VARIABLE_NAME, new EngineVariableValue(null, "String")));
//     engineIntegrationExtension.startProcessInstance(
//         processDefinition.getId(), ImmutableMap.of(STRING_VARIABLE_NAME, "validMatch"));
//     engineIntegrationExtension.startProcessInstance(
//         processDefinition.getId(), ImmutableMap.of(STRING_VARIABLE_NAME, "noMatch"));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .stringType()
//             .name(STRING_VARIABLE_NAME)
//             .operator(operator)
//             .values(filterValues)
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(expectedInstanceCount);
//   }
//
//   @Test
//   public void stringInequalityVariableFilter() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var", "value");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", "anotherValue");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", "aThirdValue");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .name("var")
//             .stringType()
//             .values(Collections.singletonList("value"))
//             .operator(NOT_IN)
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(2);
//   }
//
//   @Test
//   public void multipleStringInequalityVariableFilter() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
//     Map<String, Object> variables = new HashMap<>();
//     variables.put("var", "1");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", "2");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     variables.put("var", "3");
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     List<String> values = new ArrayList<>();
//     values.add("2");
//     values.add("1");
//
//     List<ProcessFilterDto<?>> filter =
//         ProcessFilterBuilder.filter()
//             .variable()
//             .name("var")
//             .stringType()
//             .values(values)
//             .operator(NOT_IN)
//             .add()
//             .buildList();
//
//     ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
//         evaluateReportWithFilter(processDefinition, filter);
//
//     // then
//     assertThat(result.getData()).hasSize(1);
//   }
// }
