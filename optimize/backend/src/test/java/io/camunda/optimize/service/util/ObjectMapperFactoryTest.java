/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.ReportType;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.variable.BooleanVariableFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.ConfigurationServiceBuilder;
import io.camunda.optimize.service.util.mapper.ObjectMapperFactory;
import io.camunda.optimize.service.util.mapper.OptimizeDateTimeFormatterFactory;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ObjectMapperFactoryTest {

  private ConfigurationService configurationService;
  private ObjectMapperFactory objectMapperFactory;

  @BeforeEach
  public void init() {
    configurationService =
        ConfigurationServiceBuilder.createConfiguration()
            .loadConfigurationFrom("service-config.yaml")
            .build();
    objectMapperFactory =
        new ObjectMapperFactory(
            new OptimizeDateTimeFormatterFactory().getObject(), configurationService);
  }

  /**
   * By default jackson fails if the external type id property is present but the actual property
   * not. In this case "reportType" is the external type id and "data" is the property.
   *
   * @see
   *     com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY
   */
  @Test
  public void testNoFailOnMissingReportDataAlthoughReportTypeSet() throws Exception {
    final ReportDefinitionDto reportDefinitionDto =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/single-process-report-definition-create-request.json"),
                ReportDefinitionDto.class);
    assertThat(reportDefinitionDto.isCombined()).isFalse();
    assertThat(reportDefinitionDto.getReportType()).isEqualTo(ReportType.PROCESS);
    assertThat(reportDefinitionDto)
        .isInstanceOf(SingleProcessReportDefinitionRequestDto.class)
        .satisfies(
            processDefinition -> {
              assertThat(processDefinition.getData()).isNotNull();
            });
  }

  @Test
  public void testCanDeserializeToSingleProcessReport() throws Exception {
    final SingleProcessReportDefinitionRequestDto reportDefinitionDto =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/single-process-report-definition-create-request.json"),
                SingleProcessReportDefinitionRequestDto.class);

    assertThat(reportDefinitionDto.isCombined()).isFalse();
    assertThat(reportDefinitionDto.getReportType()).isEqualTo(ReportType.PROCESS);
    assertThat(reportDefinitionDto).isInstanceOf(SingleProcessReportDefinitionRequestDto.class);
  }

  @Test
  public void testCanDeserializeToSingleDecisionReport() throws Exception {
    final SingleDecisionReportDefinitionRequestDto reportDefinitionDto =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/single-decision-report-definition-create-request.json"),
                SingleDecisionReportDefinitionRequestDto.class);

    assertThat(reportDefinitionDto.isCombined()).isFalse();
    assertThat(reportDefinitionDto.getReportType()).isEqualTo(ReportType.DECISION);
    assertThat(reportDefinitionDto).isInstanceOf(SingleDecisionReportDefinitionRequestDto.class);
  }

  @Test
  public void testCanDeserializeToCombinedProcessReport() throws Exception {
    final CombinedReportDefinitionRequestDto reportDefinitionDto =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/combined-process-report-definition-create-request.json"),
                CombinedReportDefinitionRequestDto.class);

    assertThat(reportDefinitionDto.isCombined()).isTrue();
    assertThat(reportDefinitionDto.getReportType()).isEqualTo(ReportType.PROCESS);
    assertThat(reportDefinitionDto).isInstanceOf(CombinedReportDefinitionRequestDto.class);
  }

  @Test
  public void testFilterSerialization() throws Exception {
    ProcessReportDataDto data =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream("/test/data/filter_request.json"),
                ProcessReportDataDto.class);
    assertThat(
            ((BooleanVariableFilterDataDto) data.getFilter().get(0).getData())
                .getData()
                .getValues())
        .containsExactly(true);

    data =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/filter_request_single.json"),
                ProcessReportDataDto.class);
    assertThat(
            ((BooleanVariableFilterDataDto) data.getFilter().get(0).getData())
                .getData()
                .getValues())
        .containsExactly(true);
  }

  @Test
  public void testFilterSerializationWithLowercaseType() throws Exception {
    ProcessReportDataDto data =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/filter_request_lowercase_type.json"),
                ProcessReportDataDto.class);
    assertThat(
            ((BooleanVariableFilterDataDto) data.getFilter().get(0).getData())
                .getData()
                .getValues())
        .containsExactly(true);

    data =
        createOptimizeMapper()
            .readValue(
                ObjectMapperFactoryTest.class.getResourceAsStream(
                    "/test/data/filter_request_single.json"),
                ProcessReportDataDto.class);
    assertThat(
            ((BooleanVariableFilterDataDto) data.getFilter().get(0).getData())
                .getData()
                .getValues())
        .containsExactly(true);
  }

  @Test
  public void testOptimizeMapperDateSerialization() throws Exception {
    // given
    final String dateString = "2017-12-11T17:28:38.222+0100";

    // when
    final DateHolder instance = new DateHolder();
    instance.setDate(OffsetDateTime.parse(dateString, createEngineDateFormatter()));
    final String parsedString = createOptimizeMapper().writeValueAsString(instance);

    // then
    assertThat(parsedString).contains(dateString);
  }

  @Test
  public void testOptimizeMapperDateDeserialization() throws Exception {
    // given
    final String dateString = "2017-12-11T17:28:38.222+0100";
    final OffsetDateTime expectedOffsetDateTime =
        OffsetDateTime.parse(dateString, createEngineDateFormatter());

    // when
    final OffsetDateTime parsedOffsetDateTime =
        createOptimizeMapper()
            .readValue(createDateHolderJsonString(dateString), DateHolder.class)
            .getDate();

    // then
    assertThat(parsedOffsetDateTime).isEqualTo(expectedOffsetDateTime);
  }

  @Test
  public void testEngineMapperDateSerialization() throws Exception {
    // given
    final String dateString = "2017-12-11T17:28:38.222+0100";

    // when
    final DateHolder instance = new DateHolder();
    instance.setDate(OffsetDateTime.parse(dateString, createEngineDateFormatter()));
    final String parsedString = createEngineMapper().writeValueAsString(instance);

    // then
    assertThat(parsedString).contains(dateString);
  }

  @Test
  public void testEngineMapperDateDeserialization() throws JsonProcessingException {
    // given
    final String dateString = "2017-12-11T17:28:38.222+0100";
    final OffsetDateTime expectedOffsetDateTime =
        OffsetDateTime.parse(dateString, createEngineDateFormatter());

    // when
    final OffsetDateTime parsedOffsetDateTime =
        createEngineMapper()
            .readValue(createDateHolderJsonString(dateString), DateHolder.class)
            .getDate();

    // then
    assertThat(parsedOffsetDateTime).isEqualTo(expectedOffsetDateTime);
  }

  @Test
  public void testEngineMapperDateDeserializationFromStringWithoutMillisAndTimezone()
      throws JsonProcessingException {
    // given
    final String datePattern = "yyyy-MM-dd'T'HH:mm:ss";
    configurationService.setEngineDateFormat(datePattern);

    final String dateString = "2017-12-11T17:28:38";
    final OffsetDateTime expectedOffsetDateTime =
        LocalDateTime.parse(dateString, createEngineDateFormatter())
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime();

    // when
    final OffsetDateTime parsedOffsetDateTime =
        createEngineMapper()
            .readValue(createDateHolderJsonString(dateString), DateHolder.class)
            .getDate();

    // then
    assertThat(parsedOffsetDateTime).isEqualTo(expectedOffsetDateTime);
  }

  private DateTimeFormatter createEngineDateFormatter() {
    return DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  private ObjectMapper createEngineMapper() {
    return objectMapperFactory.createEngineMapper();
  }

  private ObjectMapper createOptimizeMapper() {
    return objectMapperFactory.createOptimizeMapper();
  }

  private String createDateHolderJsonString(final String dateString) {
    return "{\"date\": \"" + dateString + "\"}";
  }

  static class DateHolder {

    private OffsetDateTime date;

    public OffsetDateTime getDate() {
      return date.truncatedTo(ChronoUnit.MILLIS);
    }

    public void setDate(final OffsetDateTime date) {
      this.date = date;
    }
  }
}
