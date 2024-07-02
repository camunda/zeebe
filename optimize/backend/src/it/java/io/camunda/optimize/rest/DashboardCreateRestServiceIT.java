/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.rest;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator.IN;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.ImmutableMap;
// import io.camunda.optimize.dto.optimize.query.IdResponseDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.filter.DashboardFilterDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardReportTileDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardTileType;
// import io.camunda.optimize.dto.optimize.query.variable.VariableType;
// import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
// import io.camunda.optimize.util.MarkdownUtil;
// import jakarta.ws.rs.core.Response;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
// import org.junit.jupiter.params.provider.ValueSource;
//
// @Tag(OPENSEARCH_PASSING)
// public class DashboardCreateRestServiceIT extends AbstractDashboardRestServiceIT {
//
//   @Test
//   public void createNewDashboardWithoutAuthentication() {
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .withoutAuthentication()
//             .buildCreateDashboardRequest(generateDashboardDefinitionDto())
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
//   }
//
//   @Test
//   public void createNewDashboard() {
//     // when
//     IdResponseDto idDto =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest()
//             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(idDto).isNotNull();
//   }
//
//   @ParameterizedTest
//   @MethodSource("validDescription")
//   public void createNewDashboardWithDescription(final String description) {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto = generateDashboardDefinitionDto();
//     dashboardDefinitionDto.setDescription(description);
//
//     // when
//     IdResponseDto idDto =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(idDto).isNotNull();
//
// assertThat(dashboardClient.getDashboard(idDto.getId()).getDescription()).isEqualTo(description);
//   }
//
//   @ParameterizedTest
//   @MethodSource("invalidDescription")
//   public void createNewDashboardWithInvalidDescription(final String description) {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto = generateDashboardDefinitionDto();
//     dashboardDefinitionDto.setDescription(description);
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void createNewManagementDashboardNotSupported() {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinition = generateDashboardDefinitionDto();
//     dashboardDefinition.setManagementDashboard(true);
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinition)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @ParameterizedTest
//   @MethodSource("validFilterCombinations")
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void createNewDashboardWithFilterSpecification(
//       List<DashboardFilterDto<?>> dashboardFilterDtos) {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(dashboardFilterDtos);
//
//     // when
//     final IdResponseDto idDto =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(idDto.getId()).isNotNull();
//     final DashboardDefinitionRestDto savedDefinition =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetDashboardRequest(idDto.getId())
//             .execute(DashboardDefinitionRestDto.class, Response.Status.OK.getStatusCode());
//     if (dashboardFilterDtos == null) {
//       assertThat(savedDefinition.getAvailableFilters()).isNull();
//     } else {
//       assertThat(savedDefinition.getAvailableFilters())
//           .containsExactlyInAnyOrderElementsOf(dashboardFilterDtos);
//     }
//   }
//
//   @ParameterizedTest
//   @MethodSource("invalidFilterCombinations")
//   public void createNewDashboardWithInvalidFilterSpecification(
//       List<DashboardFilterDto<?>> dashboardFilterDtos) {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto = generateDashboardDefinitionDto();
//     dashboardDefinitionDto.setAvailableFilters(dashboardFilterDtos);
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void createNewDashboardWithFilterSpecification_dashboardContainsExternalReport() {
//     // given
//     final List<DashboardFilterDto<?>> dashboardFilters = variableFilter();
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(dashboardFilters);
//     final DashboardReportTileDto variableReport = dashboardDefinitionDto.getTiles().get(0);
//
//     DashboardReportTileDto externalReport = new DashboardReportTileDto();
//     externalReport.setType(DashboardTileType.EXTERNAL_URL);
//     externalReport.setId("");
//     externalReport.setConfiguration(ImmutableMap.of("external",
// "https://www.tunnelsnakes.com/"));
//     dashboardDefinitionDto.setTiles(Arrays.asList(externalReport, variableReport));
//
//     // when
//     final IdResponseDto idDto =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(idDto.getId()).isNotNull();
//     final DashboardDefinitionRestDto savedDefinition =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetDashboardRequest(idDto.getId())
//             .execute(DashboardDefinitionRestDto.class, Response.Status.OK.getStatusCode());
//     assertThat(savedDefinition.getTiles())
//         .containsExactlyInAnyOrder(variableReport, externalReport);
//     assertThat(savedDefinition.getAvailableFilters())
//         .containsExactlyInAnyOrderElementsOf(dashboardFilters);
//   }
//
//   @Test
//   public void createNewDashboard_dashboardContainsTextReport() {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(Collections.emptyList());
//
//     DashboardReportTileDto textReport = new DashboardReportTileDto();
//     textReport.setType(DashboardTileType.TEXT);
//     textReport.setId("");
//     textReport.setConfiguration(MarkdownUtil.getMarkdownForTextReport("I _love_ *markdown*."));
//     dashboardDefinitionDto.setTiles(List.of(textReport));
//
//     // when
//     final IdResponseDto idDto =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(idDto.getId()).isNotNull();
//     final DashboardDefinitionRestDto savedDefinition =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetDashboardRequest(idDto.getId())
//             .execute(DashboardDefinitionRestDto.class, Response.Status.OK.getStatusCode());
//     assertThat(savedDefinition.getTiles()).containsExactlyInAnyOrder(textReport);
//   }
//
//   @ParameterizedTest
//   @MethodSource("getInvalidReportIdAndTypes")
//   public void createNewDashboard_invalidIdAndTypeCombinations(
//       final DashboardReportTileDto dashboardReportTileDto) {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(Collections.emptyList());
//
//     dashboardDefinitionDto.setTiles(List.of(dashboardReportTileDto));
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void createNewDashboard_dashboardTileDoesNotSpecifyType() {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(Collections.emptyList());
//
//     DashboardReportTileDto externalReport = new DashboardReportTileDto();
//     externalReport.setType(null);
//     externalReport.setId("");
//     externalReport.setConfiguration(ImmutableMap.of("external",
// "https://www.tunnelsnakes.com/"));
//     dashboardDefinitionDto.setTiles(List.of(externalReport));
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @ParameterizedTest
//   @ValueSource(
//       strings = {
//         // This string was reported as a vulnerability in
// https://jira.camunda.com/browse/OPT-6583
//         "javascript:alert(\"SySS Stored XSS Proof of Concept within
// domain:\\n\\n\"+document.domain)",
//         "invalidExternalUrl.com"
//       })
//   public void createNewDashboard_dashboardContainsExternalReportWithInvalidURL(final String url)
// {
//     // given
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(Collections.emptyList());
//     DashboardReportTileDto externalReport = new DashboardReportTileDto();
//     externalReport.setId("");
//     externalReport.setType(DashboardTileType.EXTERNAL_URL);
//     externalReport.setConfiguration(ImmutableMap.of("external", url));
//     dashboardDefinitionDto.setTiles(List.of(externalReport));
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void createNewDashboardWithVariableFilter_variableNameNotInContainedReport() {
//     // given
//     final List<DashboardFilterDto<?>> variableFilter = variableFilter();
//     final DashboardDefinitionRestDto dashboardDefinitionDto = new DashboardDefinitionRestDto();
//     dashboardDefinitionDto.setAvailableFilters(variableFilter);
//
//     // when
//     final ErrorResponseDto errorResponse =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());
//
//     // then
//     assertThat(errorResponse.getErrorCode()).isEqualTo("invalidDashboardVariableFilter");
//   }
//
//   @Test
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void createNewDashboardWithVariableFilter_variableValueNotInContainedReport() {
//     // given
//     final DashboardFilterDto<?> dashboardFilter =
//         createDashboardVariableFilter(
//             VariableType.STRING,
//             "stringVar",
//             IN,
//             Collections.singletonList("thisValueIsNotInReport"),
//             false);
//     final List<DashboardFilterDto<?>> dashboardFilters =
// Collections.singletonList(dashboardFilter);
//
//     final DashboardDefinitionRestDto dashboardDefinitionDto =
//         createDashboardForReportContainingAllVariables(dashboardFilters);
//
//     // when
//     final IdResponseDto idDto =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboardDefinitionDto)
//             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());
//
//     // then
//     assertThat(idDto.getId()).isNotNull();
//     final DashboardDefinitionRestDto savedDefinition =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetDashboardRequest(idDto.getId())
//             .execute(DashboardDefinitionRestDto.class, Response.Status.OK.getStatusCode());
//     assertThat(savedDefinition.getAvailableFilters())
//         .containsExactlyInAnyOrderElementsOf(dashboardFilters);
//   }
//
//   @Test
//   public void createDashboardWithRefreshRate() {
//     // given
//     final DashboardDefinitionRestDto dashboard = new DashboardDefinitionRestDto();
//     dashboard.setRefreshRateSeconds(5L);
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildCreateDashboardRequest(dashboard)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
//   }
// }
