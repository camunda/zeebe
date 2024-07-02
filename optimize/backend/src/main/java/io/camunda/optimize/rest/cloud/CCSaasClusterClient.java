/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.cloud;

import static io.camunda.optimize.dto.optimize.query.ui_configuration.AppName.CONSOLE;
import static io.camunda.optimize.dto.optimize.query.ui_configuration.AppName.MODELER;
import static io.camunda.optimize.dto.optimize.query.ui_configuration.AppName.OPERATE;
import static io.camunda.optimize.dto.optimize.query.ui_configuration.AppName.OPTIMIZE;
import static io.camunda.optimize.dto.optimize.query.ui_configuration.AppName.TASKLIST;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.ui_configuration.AppName;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.CCSaaSCondition;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(CCSaaSCondition.class)
public class CCSaasClusterClient extends AbstractCCSaaSClient {

  private static final String GET_CLUSTERS_TEMPLATE = GET_ORGS_TEMPLATE + "/clusters";
  private Map<AppName, String> webappsLinks;
  private static final Set<AppName> REQUIRED_WEBAPPS_LINKS =
      Set.of(CONSOLE, OPERATE, OPTIMIZE, MODELER, TASKLIST);

  public CCSaasClusterClient(
      final ConfigurationService configurationService, final ObjectMapper objectMapper) {
    super(objectMapper, configurationService);
    // To make sure we don't crash when an unknown app is sent, ignore the unknowns
    objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
  }

  public Map<AppName, String> getWebappLinks(final String accessToken) {
    if (MapUtils.isEmpty(webappsLinks)) {
      webappsLinks = retrieveWebappsLinks(accessToken);
    }
    return webappsLinks;
  }

  private Map<AppName, String> retrieveWebappsLinks(final String accessToken) {
    try {
      log.info("Fetching cluster metadata.");
      final HttpGet request =
          new HttpGet(
              String.format(
                  GET_CLUSTERS_TEMPLATE,
                  configurationService.getUiConfiguration().getConsoleUrl(),
                  getCloudAuthConfiguration().getOrganizationId()));
      final ClusterMetadata[] metadataForAllClusters;
      try (final CloseableHttpResponse response = performRequest(request, accessToken)) {
        if (response.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
          throw new OptimizeRuntimeException(
              String.format(
                  "Unexpected response when fetching cluster metadata: %s",
                  response.getStatusLine().getStatusCode()));
        }
        log.info("Processing response from Cluster metadata");
        metadataForAllClusters =
            objectMapper.readValue(response.getEntity().getContent(), ClusterMetadata[].class);
      }
      if (metadataForAllClusters != null) {
        final String currentClusterId = getCloudAuthConfiguration().getClusterId();
        return Arrays.stream(metadataForAllClusters)
            .filter(cm -> cm.getUuid().equals(currentClusterId))
            .findFirst()
            .map(cluster -> mapToWebappsLinks(cluster.getUrls()))
            // If we can't find cluster metadata for the current cluster, we can't return URLs
            .orElseThrow(
                () ->
                    new OptimizeRuntimeException(
                        "Fetched Cluster metadata successfully, but there was no data for the cluster "
                            + currentClusterId));
      } else {
        throw new OptimizeRuntimeException("Could not fetch Cluster metadata");
      }
    } catch (final IOException e) {
      throw new OptimizeRuntimeException("There was a problem fetching cluster metadata.", e);
    }
  }

  private Map<AppName, String> mapToWebappsLinks(final Map<AppName, String> urls) {
    urls.put(CONSOLE, configurationService.getUiConfiguration().getConsoleUrl());
    urls.put(MODELER, configurationService.getUiConfiguration().getModelerUrl());
    // remove any webapps URL the UI does not require
    return urls.entrySet().stream()
        // Null entries can happen if there is an App that is not present in the AppName Enum
        .filter(
            entry ->
                entry.getValue() != null
                    && entry.getKey() != null
                    && REQUIRED_WEBAPPS_LINKS.contains(entry.getKey()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ClusterMetadata implements Serializable {

    private String uuid;
    private Map<AppName, String> urls = new EnumMap<>(AppName.class);
  }
}
