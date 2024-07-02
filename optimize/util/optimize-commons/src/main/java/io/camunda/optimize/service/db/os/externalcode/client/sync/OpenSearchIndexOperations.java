/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.externalcode.client.sync;

import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.ReindexRequest;
import org.opensearch.client.opensearch.core.ReindexResponse;
import org.opensearch.client.opensearch.indices.AnalyzeRequest;
import org.opensearch.client.opensearch.indices.AnalyzeResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.opensearch.indices.RefreshResponse;
import org.opensearch.client.opensearch.tasks.GetTasksResponse;

@Slf4j
public class OpenSearchIndexOperations extends OpenSearchRetryOperation {

  public static final String NO_REPLICA = "0";
  public static final String NO_REFRESH = "-1";

  public OpenSearchIndexOperations(
      OpenSearchClient openSearchClient, final OptimizeIndexNameService indexNameService) {
    super(openSearchClient, indexNameService);
  }

  private static String defaultIndexErrorMessage(String index) {
    return String.format("Failed to search index: %s", index);
  }

  public Set<String> getIndexNamesWithRetries(String namePattern) {
    String prefixedNamePattern = applyIndexPrefix(namePattern);
    return executeWithRetries(
        "Get indices for " + prefixedNamePattern,
        () -> {
          try {
            final GetIndexResponse response =
                openSearchClient.indices().get(i -> i.index(prefixedNamePattern));
            return response.result().keySet();
          } catch (OpenSearchException e) {
            if (e.status() == 404) {
              return Set.of();
            }
            throw e;
          }
        });
  }

  public boolean createIndex(CreateIndexRequest createIndexRequest) throws IOException {
    return openSearchClient.indices().create(createIndexRequest).acknowledged();
  }

  public boolean indicesExist(List<String> unprefixedIndexes) throws IOException {
    List<String> indexes = applyIndexPrefix(unprefixedIndexes.toArray(new String[0]));
    ExistsRequest.Builder existsRequest = new ExistsRequest.Builder().index(indexes);
    return openSearchClient.indices().exists(existsRequest.build()).value();
  }

  public long getNumberOfDocumentsWithRetries(String... unprefixedIndexPatterns) {
    List<String> indexPatterns = applyIndexPrefix(unprefixedIndexPatterns);
    return executeWithRetries(
        "Count number of documents in " + List.of(indexPatterns),
        () -> openSearchClient.count(c -> c.index(indexPatterns)).count());
  }

  public boolean indexExists(String unprefixedIndex) {
    String index = applyIndexPrefix(unprefixedIndex);
    return safe(
        // allowNoIndices must be set to false, otherwise index names containing wildcards will
        // always return true
        () ->
            openSearchClient
                .indices()
                .exists(r -> r.index(getIndexAliasFor(index)).allowNoIndices(false))
                .value(),
        e -> defaultIndexErrorMessage(index));
  }

  public void refresh(String unprefixedIndexPattern) {
    String indexPattern = applyIndexPrefix(unprefixedIndexPattern);
    final RefreshRequest refreshRequest = new RefreshRequest.Builder().index(indexPattern).build();
    try {
      final RefreshResponse refresh = openSearchClient.indices().refresh(refreshRequest);
      if (!refresh.shards().failures().isEmpty()) {
        log.warn("Unable to refresh indices: {}", indexPattern);
      }
    } catch (Exception ex) {
      log.warn(String.format("Unable to refresh indices: %s", indexPattern), ex);
    }
  }

  public void refresh(String... unprefixedIndexPatterns) {
    List<String> indexPatterns = applyIndexPrefix(unprefixedIndexPatterns);
    final RefreshRequest refreshRequest = new RefreshRequest.Builder().index(indexPatterns).build();
    try {
      final RefreshResponse refresh = openSearchClient.indices().refresh(refreshRequest);
      if (!refresh.shards().failures().isEmpty()) {
        log.warn("Unable to refresh indices: {}", List.of(indexPatterns));
      }
    } catch (Exception ex) {
      log.warn(String.format("Unable to refresh indices: %s", List.of(indexPatterns)), ex);
    }
  }

  public void refreshWithRetries(final String unprefixedIndexPattern) {
    String indexPattern = applyIndexPrefix(unprefixedIndexPattern);
    executeWithRetries(
        "Refresh " + indexPattern,
        () -> {
          try {
            for (String index : getFilteredIndices(indexPattern)) {
              openSearchClient.indices().refresh(r -> r.index(List.of(index)));
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
  }

  private Set<String> getFilteredIndices(final String indexPattern) throws IOException {
    return openSearchClient.indices().get(i -> i.index(List.of(indexPattern))).result().keySet();
  }

  public boolean deleteIndicesWithRetries(final String... unprefixedIndexPatterns) {
    for (String unprefixedIndexPattern : unprefixedIndexPatterns) {
      String indexPattern = applyIndexPrefix(unprefixedIndexPattern);
      boolean success =
          executeWithRetries(
              "DeleteIndices " + indexPattern,
              () -> {
                for (String index : getFilteredIndices(indexPattern)) {
                  openSearchClient.indices().delete(d -> d.index(List.of(index)));
                }
                return true;
              });
      if (!success) {
        return false;
      }
    }
    return true;
  }

  public IndexSettings getIndexSettingsWithRetries(String unprefixedIndexPattern) {
    String indexName = applyIndexPrefix(unprefixedIndexPattern);
    return executeWithRetries(
        "GetIndexSettings " + indexName,
        () -> {
          final GetIndicesSettingsResponse response =
              openSearchClient.indices().getSettings(s -> s.index(List.of(indexName)));
          return response.result().get(indexName).settings();
        });
  }

  public String getOrDefaultRefreshInterval(String indexName, String defaultValue) {
    Time refreshIntervalTime = getIndexSettingsWithRetries(indexName).refreshInterval();
    String refreshInterval =
        refreshIntervalTime == null ? defaultValue : refreshIntervalTime.time();
    if (refreshInterval.trim().equals(NO_REFRESH)) {
      refreshInterval = defaultValue;
    }
    return refreshInterval;
  }

  public String getOrDefaultNumbersOfReplica(String indexName, String defaultValue) {
    String numberOfReplicasOriginal = getIndexSettingsWithRetries(indexName).numberOfReplicas();
    String numbersOfReplica =
        numberOfReplicasOriginal == null ? defaultValue : numberOfReplicasOriginal;
    if (numbersOfReplica.trim().equals(NO_REPLICA)) {
      numbersOfReplica = defaultValue;
    }
    return numbersOfReplica;
  }

  public PutIndicesSettingsResponse putSettings(PutIndicesSettingsRequest request)
      throws IOException {
    return openSearchClient.indices().putSettings(request);
  }

  public PutIndicesSettingsResponse setIndexLifeCycle(String index, String value)
      throws IOException {
    PutIndicesSettingsRequest request =
        PutIndicesSettingsRequest.of(b -> b.index(index).settings(s -> s.lifecycleName(value)));
    return putSettings(request);
  }

  public boolean setIndexSettingsFor(IndexSettings settings, String indexPattern) {
    return executeWithRetries(
        "SetIndexSettings " + indexPattern,
        () ->
            openSearchClient
                .indices()
                .putSettings(s -> s.index(indexPattern).settings(settings))
                .acknowledged());
  }

  public AnalyzeResponse analyze(AnalyzeRequest analyzeRequest) throws IOException {
    return openSearchClient.indices().analyze(analyzeRequest);
  }

  // TODO check unused
  public void reindexWithRetries(final ReindexRequest reindexRequest) {
    reindexWithRetries(reindexRequest, true);
  }

  // TODO check unused
  public void reindexWithRetries(final ReindexRequest reindexRequest, boolean checkDocumentCount) {
    executeWithRetries(
        "Reindex "
            + Arrays.asList(reindexRequest.source().index())
            + " -> "
            + reindexRequest.dest().index(),
        () -> {
          final String srcIndices = reindexRequest.source().index().get(0);
          final long srcCount = getNumberOfDocumentsWithRetries(srcIndices);
          if (checkDocumentCount) {
            final String dstIndex = reindexRequest.dest().index();
            final long dstCount = getNumberOfDocumentsWithRetries(dstIndex + "*");
            if (srcCount == dstCount) {
              log.info("Reindex of {} -> {} is already done.", srcIndices, dstIndex);
              return true;
            }
          }
          ReindexResponse response = openSearchClient.reindex(reindexRequest);

          if (response.total().equals(srcCount)) {
            String taskId = response.task() != null ? response.task() : "task:unavailable";
            logProgress(taskId, srcCount, srcCount);
            return true;
          }

          TimeUnit.of(ChronoUnit.MILLIS).sleep(2_000);
          return waitUntilTaskIsCompleted(response.task(), srcCount);
        },
        done -> !done);
  }

  // Returns if task is completed under this conditions:
  // - If the response is empty we can immediately return false to force a new reindex in outer
  // retry loop
  // - If the response has a status with uncompleted flag and a sum of changed documents
  // (created,updated and deleted documents) not equal to to total documents
  //   we need to wait and poll again the task status
  private boolean waitUntilTaskIsCompleted(String taskId, long srcCount) {
    final GetTasksResponse taskResponse = waitTaskCompletion(taskId);

    if (taskResponse != null) {
      logProgress(taskId, taskResponse.response().total(), srcCount);

      final long total = taskResponse.response().total();
      log.info("Source docs: {}, Migrated docs: {}", srcCount, total);
      return total == srcCount;
    } else {
      // need to reindex again
      return false;
    }
  }

  private void logProgress(String taskId, long processed, long srcCount) {
    double progress = processed * 100.00 / srcCount;
    log.info("TaskId: {}, Progress: {}%", taskId, String.format("%.2f", progress));
  }

  public GetIndexResponse get(GetIndexRequest.Builder requestBuilder) {
    GetIndexRequest request = applyIndexPrefix(requestBuilder).build();
    return safe(
        () -> openSearchClient.indices().get(request),
        e -> "Failed to get index " + request.index());
  }
}
