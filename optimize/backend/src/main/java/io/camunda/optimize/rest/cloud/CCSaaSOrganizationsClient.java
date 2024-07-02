/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.CCSaaSCondition;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(CCSaaSCondition.class)
public class CCSaaSOrganizationsClient extends AbstractCCSaaSClient {

  public CCSaaSOrganizationsClient(
      final ConfigurationService configurationService, final ObjectMapper objectMapper) {
    super(objectMapper, configurationService);
  }

  public Optional<String> getSalesPlanType(final String accessToken) {
    try {
      log.info("Fetching cloud organisation.");
      final HttpGet request =
          new HttpGet(
              String.format(
                  GET_ORGS_TEMPLATE,
                  getCloudUsersConfiguration().getAccountsUrl(),
                  getCloudAuthConfiguration().getOrganizationId()));
      try (final CloseableHttpResponse response = performRequest(request, accessToken)) {
        if (response.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
          throw new OptimizeRuntimeException(
              String.format(
                  "Unexpected response when fetching cloud organisation: %s",
                  response.getStatusLine().getStatusCode()));
        }
        final AccountsOrganisationResponse responseEntity =
            objectMapper.readValue(
                response.getEntity().getContent(), AccountsOrganisationResponse.class);
        return Optional.ofNullable(responseEntity)
            .flatMap(AccountsOrganisationResponse::getSalesPlan)
            .flatMap(AccountsSalesPlanDto::getType);
      }
    } catch (IOException e) {
      throw new OptimizeRuntimeException("There was a problem fetching the cloud organisation.", e);
    }
  }

  @Data
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor
  public static class AccountsOrganisationResponse {
    private Optional<AccountsSalesPlanDto> salesPlan;
  }

  @Data
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor
  public static class AccountsSalesPlanDto {
    private Optional<String> type;
  }
}
