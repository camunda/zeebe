/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// // TODO recreate C8 IT equivalent of this with #13337
// // package io.camunda.optimize.rest.eventprocess;
// //
// // import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// // import static io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto.Fields.id;
// // import static io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto.Fields.source;
// // import static io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto.Fields.specversion;
// // import static io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto.Fields.traceid;
// // import static io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto.Fields.type;
// // import static io.camunda.optimize.rest.IngestionRestService.EVENT_BATCH_SUB_PATH;
// // import static io.camunda.optimize.rest.IngestionRestService.INGESTION_PATH;
// // import static io.camunda.optimize.rest.constants.RestConstants.AUTH_COOKIE_TOKEN_VALUE_PREFIX;
// // import static
// io.camunda.optimize.rest.providers.BeanConstraintViolationExceptionHandler.THE_REQUEST_BODY_WAS_INVALID;
// // import static java.util.stream.Collectors.toList;
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import com.fasterxml.jackson.core.JsonProcessingException;
// // import io.camunda.optimize.AbstractPlatformIT;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
// // import io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
// // import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
// // import io.camunda.optimize.dto.optimize.rest.ValidationErrorResponseDto;
// // import io.camunda.optimize.jetty.IngestionQoSFilter;
// // import io.camunda.optimize.jetty.MaxRequestSizeFilter;
// // import io.camunda.optimize.service.security.util.LocalDateUtil;
// // import io.camunda.optimize.test.it.extension.IntegrationTestConfigurationUtil;
// // import io.camunda.optimize.util.SuppressionConstants;
// // import jakarta.ws.rs.core.HttpHeaders;
// // import jakarta.ws.rs.core.MediaType;
// // import jakarta.ws.rs.core.Response;
// // import java.io.IOException;
// // import java.time.Instant;
// // import java.time.OffsetDateTime;
// // import java.util.Collections;
// // import java.util.List;
// // import java.util.stream.Collectors;
// // import java.util.stream.IntStream;
// // import java.util.stream.Stream;
// // import org.apache.http.client.methods.CloseableHttpResponse;
// // import org.apache.http.client.methods.HttpPut;
// // import org.apache.http.impl.client.CloseableHttpClient;
// // import org.apache.http.impl.client.HttpClients;
// // import org.apache.http.protocol.HttpProcessorBuilder;
// // import org.apache.http.protocol.RequestTargetHost;
// // import org.junit.jupiter.api.BeforeEach;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// // import org.junit.jupiter.params.ParameterizedTest;
// // import org.junit.jupiter.params.provider.MethodSource;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class EventIngestionRestIT extends AbstractPlatformIT {
// //
// //   @BeforeEach
// //   public void before() {
// //     LocalDateUtil.setCurrentTime(OffsetDateTime.now());
// //   }
// //
// //   @Test
// //   public void ingestEventBatch() {
// //     // given
// //     final List<CloudEventRequestDto> eventDtos =
// //         IntStream.range(0, 10)
// //             .mapToObj(operand -> ingestionClient.createCloudEventDto())
// //             .collect(toList());
// //
// //     // when
// //     final Response ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(eventDtos, getAccessToken())
// //             .execute();
// //
// //     // then
// //
// assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     assertEventDtosArePersisted(eventDtos);
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_emptyBatch() {
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(Collections.emptyList(), getAccessToken())
// //             .execute();
// //
// //     // then
// //
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //     assertThat(databaseIntegrationTestExtension.getAllStoredExternalEvents()).isEmpty();
// //   }
// //
// //   @Test
// //   public void ingestEventBatchWithPlainJsonContentType() {
// //     // given
// //     final List<CloudEventRequestDto> eventDtos =
// //         IntStream.range(0, 10)
// //             .mapToObj(operand -> ingestionClient.createCloudEventDto())
// //             .collect(toList());
// //
// //     // when
// //     final Response ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatchWithMediaType(
// //                 eventDtos, getAccessToken(), MediaType.APPLICATION_JSON)
// //             .execute();
// //
// //     // then
// //
// assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     assertEventDtosArePersisted(eventDtos);
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_maxRequestsConfiguredReached() {
// //     // given
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getEventIngestionConfiguration()
// //         .setMaxRequests(0);
// //
// //     final List<CloudEventRequestDto> eventDtos =
// //         IntStream.range(0, 1)
// //             .mapToObj(operand -> ingestionClient.createCloudEventDto())
// //             .collect(toList());
// //
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(eventDtos, getAccessToken())
// //             .execute();
// //
// //     // then
// //     assertThat(response)
// //         .satisfies(
// //             httpResponse -> {
// //               assertThat(httpResponse.getStatus())
// //                   .isEqualTo(Response.Status.TOO_MANY_REQUESTS.getStatusCode());
// //               assertThat(httpResponse.getHeaderString(HttpHeaders.RETRY_AFTER))
// //                   .isEqualTo(IngestionQoSFilter.RETRY_AFTER_SECONDS);
// //             });
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_customSecret() {
// //     // given
// //     final CloudEventRequestDto eventDto = ingestionClient.createCloudEventDto();
// //
// //     final String customSecret = "mySecret";
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getOptimizeApiConfiguration()
// //         .setAccessToken(customSecret);
// //
// //     // when
// //     final Response ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(Collections.singletonList(eventDto), customSecret)
// //             .execute();
// //
// //     // then
// //
// assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     assertEventDtosArePersisted(Collections.singletonList(eventDto));
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_customSecretUsingBearerScheme() {
// //     // given
// //     final CloudEventRequestDto eventDto = ingestionClient.createCloudEventDto();
// //
// //     final String customSecret = "mySecret";
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getOptimizeApiConfiguration()
// //         .setAccessToken(customSecret);
// //
// //     // when
// //     final Response ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(Collections.singletonList(eventDto), customSecret)
// //             .execute();
// //
// //     // then
// //
// assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     assertEventDtosArePersisted(Collections.singletonList(eventDto));
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_notAuthorized() {
// //     // given
// //     final List<CloudEventRequestDto> eventDtos =
// //         IntStream.range(0, 2)
// //             .mapToObj(operand -> ingestionClient.createCloudEventDto())
// //             .collect(toList());
// //
// //     // when
// //     final Response ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(eventDtos, "wroooong")
// //             .execute();
// //
// //     // then
// //
// assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
// //
// //     assertEventDtosArePersisted(Collections.emptyList());
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_limitExceeded() {
// //     // given
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getEventIngestionConfiguration()
// //         .setMaxBatchRequestBytes(1L);
// //
// //     final List<CloudEventRequestDto> eventDtos =
// //         IntStream.range(0, 2)
// //             .mapToObj(operand -> ingestionClient.createCloudEventDto())
// //             .collect(toList());
// //
// //     // when
// //     final ErrorResponseDto ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(eventDtos, getAccessToken())
// //             .execute(
// //                 ErrorResponseDto.class,
// Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());
// //
// //     // then
// //     assertThat(ingestResponse.getErrorMessage()).contains("Request too large");
// //
// //     assertEventDtosArePersisted(Collections.emptyList());
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_contentLengthHeaderMissing() throws IOException {
// //     // this is a custom apache client that does not send the content-length header
// //     try (final CloseableHttpClient httpClient =
// //         HttpClients.custom()
// //             .setHttpProcessor(HttpProcessorBuilder.create().addAll(new
// RequestTargetHost()).build())
// //             .build()) {
// //       final HttpPut httpPut =
// //           new HttpPut(
// //               IntegrationTestConfigurationUtil.getEmbeddedOptimizeRestApiEndpoint(
// //                       embeddedOptimizeExtension.getApplicationContext())
// //                   + INGESTION_PATH
// //                   + EVENT_BATCH_SUB_PATH);
// //       httpPut.addHeader(
// //           HttpHeaders.AUTHORIZATION, AUTH_COOKIE_TOKEN_VALUE_PREFIX + getAccessToken());
// //       final CloseableHttpResponse response = httpClient.execute(httpPut);
// //
// //       // then
// //       assertThat(response.getStatusLine().getStatusCode())
// //           .isEqualTo(Response.Status.LENGTH_REQUIRED.getStatusCode());
// //       final ErrorResponseDto errorResponseDto =
// //           embeddedOptimizeExtension
// //               .getObjectMapper()
// //               .readValue(response.getEntity().getContent(), ErrorResponseDto.class);
// //
// //       assertThat(errorResponseDto.getErrorMessage())
// //           .isEqualTo(MaxRequestSizeFilter.MESSAGE_NO_CONTENT_LENGTH);
// //
// //       assertEventDtosArePersisted(Collections.emptyList());
// //     }
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_omitOptionalProperties() {
// //     // given
// //     final CloudEventRequestDto eventDto = ingestionClient.createCloudEventDto();
// //     eventDto.setGroup(null);
// //     eventDto.setData(null);
// //     // time will get dynamically assigned if not present
// //     LocalDateUtil.setCurrentTime(OffsetDateTime.now());
// //     eventDto.setTime(null);
// //
// //     // when
// //     final Response ingestResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(Collections.singletonList(eventDto), getAccessToken())
// //             .execute();
// //
// //     // then
// //
// assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     assertEventDtosArePersisted(Collections.singletonList(eventDto));
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_rejectMandatoryPropertiesNull() {
// //     // given
// //     final CloudEventRequestDto eventDto = ingestionClient.createCloudEventDto();
// //     eventDto.setSpecversion(null);
// //     eventDto.setId(null);
// //     eventDto.setType(null);
// //     eventDto.setSource(null);
// //     eventDto.setTraceid(null);
// //
// //     // when
// //     final ValidationErrorResponseDto ingestErrorResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(Collections.singletonList(eventDto), getAccessToken())
// //             .execute(ValidationErrorResponseDto.class,
// Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     // then
// //     assertThat(ingestErrorResponse.getErrorMessage()).isEqualTo(THE_REQUEST_BODY_WAS_INVALID);
// //     assertThat(ingestErrorResponse.getValidationErrors()).hasSize(5);
// //     assertThat(
// //             ingestErrorResponse.getValidationErrors().stream()
// //                 .map(ValidationErrorResponseDto.ValidationError::getProperty)
// //                 .map(property -> property.split("\\.")[1])
// //                 .collect(toList()))
// //         .contains(specversion, id, type, source, traceid);
// //     assertThat(
// //             ingestErrorResponse.getValidationErrors().stream()
// //                 .map(ValidationErrorResponseDto.ValidationError::getErrorMessage)
// //                 .collect(toList()))
// //         .doesNotContainNull();
// //
// //     assertEventDtosArePersisted(Collections.emptyList());
// //   }
// //
// //   @ParameterizedTest
// //   @MethodSource("invalidRFC3339EventTimes")
// //   public void ingestEventBatch_nonCompliantDateFormat(final Object time)
// //       throws JsonProcessingException {
// //     // given
// //     final CloudEventRequestDto eventDto = ingestionClient.createCloudEventDto();
// //
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventWithBody(convertToJsonBodyWithTime(eventDto, time),
// getAccessToken())
// //             .execute(Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     assertEventDtosArePersisted(Collections.emptyList());
// //   }
// //
// //   private static Stream<String> invalidEventSource() {
// //     return Stream.of("camunda", "Camunda", "");
// //   }
// //
// //   @ParameterizedTest
// //   @MethodSource("invalidEventSource")
// //   public void ingestEventBatch_rejectInvalidPropertyValues(final String invalidSourceValue) {
// //     // given
// //     final CloudEventRequestDto eventDto = ingestionClient.createCloudEventDto();
// //     eventDto.setId("  ");
// //     eventDto.setSpecversion("0");
// //     eventDto.setType("");
// //     eventDto.setSource(invalidSourceValue);
// //     eventDto.setTraceid("");
// //
// //     // when
// //     final ValidationErrorResponseDto ingestErrorResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(Collections.singletonList(eventDto), getAccessToken())
// //             .execute(ValidationErrorResponseDto.class,
// Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     // then
// //     assertThat(ingestErrorResponse.getErrorMessage()).isEqualTo(THE_REQUEST_BODY_WAS_INVALID);
// //     assertThat(ingestErrorResponse.getValidationErrors())
// //         .hasSize(5)
// //         .extracting(ValidationErrorResponseDto.ValidationError::getProperty)
// //         .map(property -> property.split("\\.")[1])
// //         .containsExactlyInAnyOrder(id, specversion, type, source, traceid);
// //     assertThat(ingestErrorResponse.getValidationErrors())
// //         .extracting(ValidationErrorResponseDto.ValidationError::getErrorMessage)
// //         .doesNotContainNull();
// //
// //     assertEventDtosArePersisted(Collections.emptyList());
// //   }
// //
// //   @Test
// //   public void ingestEventBatch_rejectInvalidPropertyValueOfSpecificEntry() {
// //     // given
// //     final List<CloudEventRequestDto> eventDtos =
// //         IntStream.range(0, 2)
// //             .mapToObj(operand -> ingestionClient.createCloudEventDto())
// //             .collect(toList());
// //
// //     final CloudEventRequestDto invalidEventDto1 = ingestionClient.createCloudEventDto();
// //     invalidEventDto1.setId(null);
// //     eventDtos.add(invalidEventDto1);
// //
// //     // when
// //     final ValidationErrorResponseDto ingestErrorResponse =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildIngestEventBatch(eventDtos, getAccessToken())
// //             .execute(ValidationErrorResponseDto.class,
// Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     // then
// //     assertThat(ingestErrorResponse.getErrorMessage()).isEqualTo(THE_REQUEST_BODY_WAS_INVALID);
// //     assertThat(ingestErrorResponse.getValidationErrors())
// //         .hasSize(1)
// //         .extracting(ValidationErrorResponseDto.ValidationError::getProperty)
// //         .containsExactly("element[2]." + id);
// //     assertThat(ingestErrorResponse.getValidationErrors())
// //         .extracting(ValidationErrorResponseDto.ValidationError::getErrorMessage)
// //         .doesNotContainNull();
// //
// //     assertEventDtosArePersisted(Collections.emptyList());
// //   }
// //
// //   @SuppressWarnings(SuppressionConstants.UNUSED)
// //   private static Stream<Object> invalidRFC3339EventTimes() {
// //     return Stream.of(
// //         "5",
// //         12345,
// //         "8/17/2018 7:00:00 AM",
// //         "2017-05-01 16:23:12Z", // RFC3339 specifies using a 'T' rather than a space between
// date
// //         // and time
// //         Instant.now().toEpochMilli(),
// //         String.valueOf(Instant.now().toEpochMilli()));
// //   }
// //
// //   private String convertToJsonBodyWithTime(
// //       final CloudEventRequestDto cloudEventDto, final Object time) throws
// JsonProcessingException {
// //     return "[ {\n"
// //         + "  \"id\" : \""
// //         + cloudEventDto.getId()
// //         + "\",\n"
// //         + "  \"source\" : \""
// //         + cloudEventDto.getSource()
// //         + "\",\n"
// //         + "  \"specversion\" : \""
// //         + cloudEventDto.getSpecversion()
// //         + "\",\n"
// //         + "  \"type\" : \""
// //         + cloudEventDto.getType()
// //         + "\",\n"
// //         + "  \"time\" : "
// //         + embeddedOptimizeExtension.getObjectMapper().writeValueAsString(time)
// //         + ",\n"
// //         + "  \"traceid\" : \""
// //         + cloudEventDto.getTraceid()
// //         + "\",\n"
// //         + "} ]";
// //   }
// //
// //   private void assertEventDtosArePersisted(final List<CloudEventRequestDto> cloudEventDtos) {
// //     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
// //     final Instant rightNow = LocalDateUtil.getCurrentDateTime().toInstant();
// //     final List<EventDto> expectedEventDtos =
// //         cloudEventDtos.stream()
// //             .map(
// //                 cloudEventDto ->
// //                     EventDto.builder()
// //                         .id(cloudEventDto.getId())
// //                         .eventName(cloudEventDto.getType())
// //                         .timestamp(cloudEventDto.getTime().orElse(rightNow).toEpochMilli())
// //                         .traceId(cloudEventDto.getTraceid())
// //                         .group(cloudEventDto.getGroup().orElse(null))
// //                         .source(cloudEventDto.getSource())
// //                         .data(cloudEventDto.getData().orElse(null))
// //                         .ingestionTimestamp(rightNow.toEpochMilli())
// //                         .build())
// //             .collect(Collectors.toList());
// //     final List<EventDto> indexedEventDtos =
// //         databaseIntegrationTestExtension.getAllStoredExternalEvents();
// //     assertThat(indexedEventDtos).containsExactlyInAnyOrderElementsOf(expectedEventDtos);
// //   }
// //
// //   private String getAccessToken() {
// //     return embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getOptimizeApiConfiguration()
// //         .getAccessToken();
// //   }
// // }
