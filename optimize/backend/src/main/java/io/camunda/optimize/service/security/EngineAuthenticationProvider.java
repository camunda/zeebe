/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.security;

import static io.camunda.optimize.service.util.importing.EngineConstants.USER_VALIDATION_ENDPOINT;

import io.camunda.optimize.dto.engine.AuthenticationResultDto;
import io.camunda.optimize.dto.optimize.query.security.CredentialsRequestDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class EngineAuthenticationProvider {

  public static final String INVALID_CREDENTIALS_ERROR_MESSAGE =
      "The provided credentials are invalid. Please check your username and password.";
  private static final String CONNECTION_WAS_REFUSED_ERROR =
      "Connection to engine was refused! Please check if the engine is still running.";

  private final ConfigurationService configurationService;

  public AuthenticationResultDto performAuthenticationCheck(
      CredentialsRequestDto credentialsRequestDto, EngineContext engineContext) {
    try {
      final Response response =
          engineContext
              .getEngineClient()
              .target(
                  configurationService.getEngineRestApiEndpointOfCustomEngine(
                      engineContext.getEngineAlias()))
              .path(USER_VALIDATION_ENDPOINT)
              .request(MediaType.APPLICATION_JSON)
              .post(Entity.json(credentialsRequestDto));

      if (responseIsSuccessful(response)) {
        AuthenticationResultDto authResult = response.readEntity(AuthenticationResultDto.class);
        authResult.setEngineAlias(engineContext.getEngineAlias());
        if (!authResult.isAuthenticated()) {
          authResult.setErrorMessage(INVALID_CREDENTIALS_ERROR_MESSAGE);
        }
        return authResult;
      } else {
        log.error(
            "Could not validate user [{}] against the engine [{}]. "
                + "Maybe you did not provide a user or password or the user is locked",
            credentialsRequestDto.getUsername(),
            engineContext.getEngineAlias());
        // read Exception from the engine response
        // and rethrow it to forward the error message to the client
        // e.g when the user is locked, the error message will contain corresponding information
        Exception runtimeException = response.readEntity(RuntimeException.class);
        return getAuthenticationResultFromError(
            credentialsRequestDto, engineContext, runtimeException);
      }
    } catch (ProcessingException | OptimizeRuntimeException e) {
      String errorMessage =
          String.format(
              "Could not authenticated against engine [%s]. " + CONNECTION_WAS_REFUSED_ERROR,
              engineContext.getEngineAlias());
      OptimizeRuntimeException optimizeEx = new OptimizeRuntimeException(errorMessage, e);
      return getAuthenticationResultFromError(credentialsRequestDto, engineContext, optimizeEx);
    }
  }

  private AuthenticationResultDto getAuthenticationResultFromError(
      final CredentialsRequestDto credentialsRequestDto,
      final EngineContext engineContext,
      final Exception exception) {

    return AuthenticationResultDto.builder()
        .isAuthenticated(false)
        .authenticatedUser(credentialsRequestDto.getUsername())
        .engineAlias(engineContext.getEngineAlias())
        .errorMessage(exception.getMessage())
        .build();
  }

  private boolean responseIsSuccessful(Response response) {
    return response.getStatus() < 300;
  }
}
