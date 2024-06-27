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
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.service.LocalizationService;
// import io.camunda.optimize.util.FileReaderUtil;
// import jakarta.ws.rs.core.Response;
// import lombok.SneakyThrows;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
//
// @Tag(OPENSEARCH_PASSING)
// public class LocalizationRestServiceIT extends AbstractPlatformIT {
//
//   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
//
//   @ParameterizedTest
//   @MethodSource("defaultLocales")
//   public void getLocalizationFile(final String localeCode) {
//     // given
//     final JsonNode expectedLocaleJson = getExpectedLocalizationFile(localeCode);
//
//     // when
//     final JsonNode localeJson = localizationClient.getLocalizationJson(localeCode);
//
//     // then
//     assertThat(localeJson).isEqualTo(expectedLocaleJson);
//   }
//
//   @Test
//   public void getFallbackLocalizationForInvalidCode() {
//     // given
//     final String localeCode = "xyz";
//     final JsonNode expectedLocaleJson =
//         getExpectedLocalizationFile(
//             embeddedOptimizeExtension.getConfigurationService().getFallbackLocale());
//
//     // when
//     final JsonNode localeJson = localizationClient.getLocalizationJson(localeCode);
//
//     // then
//     assertThat(localeJson).isEqualTo(expectedLocaleJson);
//   }
//
//   @Test
//   public void getFallbackLocalizationForMissingCode() {
//     // given
//     final JsonNode expectedLocaleJson =
//         getExpectedLocalizationFile(
//             embeddedOptimizeExtension.getConfigurationService().getFallbackLocale());
//
//     // when
//     final JsonNode localeJson = localizationClient.getLocalizationJson(null);
//
//     // then
//     assertThat(localeJson).isEqualTo(expectedLocaleJson);
//   }
//
//   @Test
//   public void getErrorOnLocalizationFileGone() {
//     // given
//     final String localeCode = "xyz";
//     embeddedOptimizeExtension.getConfigurationService().getAvailableLocales().add(localeCode);
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildGetLocalizationRequest(localeCode)
//             .execute();
//
//     // then
//     assertThat(response.getStatus())
//         .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//   }
//
//   @SneakyThrows
//   private JsonNode getExpectedLocalizationFile(final String locale) {
//     return OBJECT_MAPPER.readValue(
//         FileReaderUtil.readFile("/" + LocalizationService.LOCALIZATION_PATH + locale + ".json"),
//         JsonNode.class);
//   }
//
//   private static String[] defaultLocales() {
//     return new String[] {"en", "de"};
//   }
// }
