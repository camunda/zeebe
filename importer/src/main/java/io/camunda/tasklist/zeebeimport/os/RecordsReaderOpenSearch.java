/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.tasklist.zeebeimport.os;

import static io.camunda.tasklist.util.OpenSearchUtil.QUERY_MAX_SIZE;
import static io.camunda.tasklist.util.OpenSearchUtil.SCROLL_KEEP_ALIVE_MS;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import io.camunda.tasklist.Metrics;
import io.camunda.tasklist.data.conditionals.OpenSearchCondition;
import io.camunda.tasklist.exceptions.NoSuchIndexException;
import io.camunda.tasklist.exceptions.TasklistRuntimeException;
import io.camunda.tasklist.schema.indices.ImportPositionIndex;
import io.camunda.tasklist.util.OpenSearchUtil;
import io.camunda.tasklist.zeebe.ImportValueType;
import io.camunda.tasklist.zeebeimport.ImportBatch;
import io.camunda.tasklist.zeebeimport.RecordsReaderAbstract;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch.core.ScrollRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Represents Zeebe data reader for one partition and one value type. After reading the data is also
 * schedules the jobs for import execution. Each reader can have it's own backoff, so that we make a
 * pause in case there is no data currently for given partition and value type.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
@Conditional(OpenSearchCondition.class)
public class RecordsReaderOpenSearch extends RecordsReaderAbstract {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordsReaderOpenSearch.class);

  @Autowired
  @Qualifier("zeebeOsClient")
  private OpenSearchClient zeebeOsClient;

  public RecordsReaderOpenSearch(int partitionId, ImportValueType importValueType, int queueSize) {
    super(partitionId, importValueType, queueSize);
  }

  public ImportBatch readNextBatchBySequence(final Long fromSequence, final Long toSequence)
      throws NoSuchIndexException {
    final String aliasName =
        importValueType.getAliasName(tasklistProperties.getZeebeOpenSearch().getPrefix());
    final int batchSize = tasklistProperties.getZeebeOpenSearch().getBatchSize();
    final long lessThanEqualsSequence;
    final int maxNumberOfHits;

    if (toSequence != null && toSequence > 0) {
      // in worst case all the records are duplicated
      maxNumberOfHits = (int) ((toSequence - fromSequence) * 2);
      lessThanEqualsSequence = toSequence;
      LOGGER.debug(
          "Import batch reread was called. Data type {}, partitionId {}, sequence {}, lastSequence {}, maxNumberOfHits {}.",
          importValueType,
          partitionId,
          fromSequence,
          toSequence,
          maxNumberOfHits);
    } else {
      maxNumberOfHits = batchSize;
      lessThanEqualsSequence = fromSequence + batchSize;
    }

    final SearchRequest searchRequest =
        new SearchRequest.Builder()
            .sort(
                s ->
                    s.field(
                        FieldSort.of(
                            f -> f.field(ImportPositionIndex.SEQUENCE).order(SortOrder.Asc))))
            .query(
                q ->
                    q.range(
                        range ->
                            range
                                .field(ImportPositionIndex.SEQUENCE)
                                .gt(JsonData.of(fromSequence))
                                .lte(JsonData.of(lessThanEqualsSequence))))
            .size(maxNumberOfHits >= QUERY_MAX_SIZE ? QUERY_MAX_SIZE : maxNumberOfHits)
            .routing(String.valueOf(partitionId))
            .requestCache(false)
            .scroll(Time.of(t -> t.time(SCROLL_KEEP_ALIVE_MS)))
            .index(aliasName)
            .build();

    try {
      final Hit[] hits =
          withTimerSearchHits(() -> read(searchRequest, maxNumberOfHits >= QUERY_MAX_SIZE));
      return createImportBatch(hits);
    } catch (OpenSearchException ex) {
      if (ex.getMessage().contains("no such index")) {
        throw new NoSuchIndexException();
      } else {
        final String message =
            String.format(
                "Exception occurred, while obtaining next Zeebe records batch: %s",
                ex.getMessage());
        throw new TasklistRuntimeException(message, ex);
      }
    } catch (Exception e) {
      final String message =
          String.format(
              "Exception occurred, while obtaining next Zeebe records batch: %s", e.getMessage());
      throw new TasklistRuntimeException(message, e);
    }
  }

  private Hit[] withTimerSearchHits(Callable<Hit[]> callable) throws Exception {
    return metrics
        .getTimer(
            Metrics.TIMER_NAME_IMPORT_QUERY,
            Metrics.TAG_KEY_TYPE,
            importValueType.name(),
            Metrics.TAG_KEY_PARTITION,
            String.valueOf(partitionId))
        .recordCallable(callable);
  }

  private ImportBatch createImportBatch(SearchResponse searchResponse) {
    final List<Hit> hits = searchResponse.hits().hits();
    String indexName = null;
    if (hits.size() > 0) {
      indexName = hits.get(hits.size() - 1).index();
    }
    return new ImportBatchOpenSearch(partitionId, importValueType, hits, indexName);
  }

  private ImportBatch createImportBatch(Hit[] hits) {
    String indexName = null;
    if (hits.length > 0) {
      indexName = hits[hits.length - 1].index();
    }
    return new ImportBatchOpenSearch(partitionId, importValueType, Arrays.asList(hits), indexName);
  }

  private Hit[] read(SearchRequest searchRequest, boolean scrollNeeded) throws IOException {
    String scrollId = null;
    try {

      if (scrollNeeded) {
        searchRequest.scroll();
      }
      SearchResponse<Object> response = zeebeOsClient.search(searchRequest, Object.class);

      final List<Hit> searchHits = new ArrayList<>(response.hits().hits());

      if (scrollNeeded) {
        scrollId = response.scrollId();
        do {
          final ScrollRequest scrollRequest =
              new ScrollRequest.Builder()
                  .scrollId(scrollId)
                  .scroll(Time.of(t -> t.time(SCROLL_KEEP_ALIVE_MS)))
                  .build();

          response = zeebeOsClient.scroll(scrollRequest, Object.class);
          scrollId = response.scrollId();
          searchHits.addAll(response.hits().hits());
        } while (response.hits().hits().size() != 0);
      }
      return searchHits.toArray(new Hit[0]);
    } finally {
      if (scrollId != null) {
        OpenSearchUtil.clearScroll(scrollId, zeebeOsClient);
      }
    }
  }

  public ImportBatch readNextBatchByPositionAndPartition(long positionFrom, Long positionTo)
      throws NoSuchIndexException {
    final String aliasName =
        importValueType.getAliasName(tasklistProperties.getZeebeOpenSearch().getPrefix());
    try {

      final SearchRequest searchRequest = createSearchQuery(aliasName, positionFrom, positionTo);

      final SearchResponse searchResponse =
          withTimer(() -> zeebeOsClient.search(searchRequest, Object.class));

      return createImportBatch(searchResponse);

    } catch (OpenSearchException ex) {
      if (ex.getMessage().contains("no such index")) {
        LOGGER.warn("No index found for alias '{}'", aliasName);
        throw new NoSuchIndexException();
      } else {
        final String message =
            String.format(
                "Exception occurred for alias [%s], while obtaining next Zeebe records batch: %s",
                aliasName, ex.getMessage());
        throw new TasklistRuntimeException(message, ex);
      }
    } catch (Exception e) {
      final String message =
          String.format(
              "Exception occurred for alias [%s], while obtaining next Zeebe records batch: %s",
              aliasName, e.getMessage());
      throw new TasklistRuntimeException(message, e);
    }
  }

  private SearchResponse withTimer(Callable<SearchResponse> callable) throws Exception {
    return metrics.getTimer(Metrics.TIMER_NAME_IMPORT_QUERY).recordCallable(callable);
  }

  private SearchRequest createSearchQuery(String aliasName, Long positionFrom, Long positionTo) {
    final RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
    rangeQuery.field(ImportPositionIndex.POSITION).gt(JsonData.of(positionFrom));
    if (positionTo != null) {
      rangeQuery.lte(JsonData.of(positionTo));
    }

    final Query query =
        OpenSearchUtil.joinWithAnd(
            new Query.Builder().range(rangeQuery.build()),
            new Query.Builder()
                .term(
                    term -> term.field(PARTITION_ID_FIELD_NAME).value(FieldValue.of(partitionId))));

    final SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder
        .query(query)
        .index(aliasName)
        .sort(s -> s.field(f -> f.field(ImportPositionIndex.POSITION).order(SortOrder.Asc)));

    if (positionTo == null) {
      searchRequestBuilder.size(tasklistProperties.getZeebeOpenSearch().getBatchSize());
    } else {
      LOGGER.debug(
          "Import batch reread was called. Data type {}, partitionId {}, positionFrom {}, positionTo {}.",
          importValueType,
          partitionId,
          positionFrom,
          positionTo);
      final int size = (int) (positionTo - positionFrom);
      searchRequestBuilder.size(size <= 0 || size > QUERY_MAX_SIZE ? QUERY_MAX_SIZE : size);
    }

    return searchRequestBuilder.routing(String.valueOf(partitionId)).requestCache(false).build();
  }
}
