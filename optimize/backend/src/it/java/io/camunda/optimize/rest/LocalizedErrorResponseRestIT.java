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
// import static io.camunda.optimize.rest.providers.GenericExceptionMapper.NOT_FOUND_ERROR_CODE;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
// import jakarta.ws.rs.HttpMethod;
// import jakarta.ws.rs.core.Response;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class LocalizedErrorResponseRestIT extends AbstractPlatformIT {
//
//   @Test
//   public void fallbackLocaleMessageIsResolved() {
//     // given
//
//     // when
//     final ErrorResponseDto errorResponseDto = executeInvalidPathRequest();
//
//     // then
//     assertThat(errorResponseDto)
//         .usingRecursiveComparison()
//         .ignoringExpectedNullFields()
//         .isEqualTo(
//             new ErrorResponseDto(
//                 NOT_FOUND_ERROR_CODE,
//                 "The server could not find the requested resource.",
//                 null,
//                 null));
//   }
//
//   @Test
//   public void customFallbackLocaleMessageIsResolved() {
//     // given
//     embeddedOptimizeExtension.getConfigurationService().setFallbackLocale("de");
//
//     // when
//     final ErrorResponseDto errorResponseDto = executeInvalidPathRequest();
//
//     // then
//     assertThat(errorResponseDto)
//         .usingRecursiveComparison()
//         .ignoringExpectedNullFields()
//         .isEqualTo(
//             new ErrorResponseDto(
//                 NOT_FOUND_ERROR_CODE,
//                 "Der Server konnte die angeforderte Seite oder Datei nicht finden.",
//                 null,
//                 null));
//   }
//
//   private ErrorResponseDto executeInvalidPathRequest() {
//     return embeddedOptimizeExtension
//         .getRequestExecutor()
//         .buildGenericRequest(HttpMethod.GET, "/api/doesNotExist", null)
//         .execute(ErrorResponseDto.class, Response.Status.NOT_FOUND.getStatusCode());
//   }
// }
