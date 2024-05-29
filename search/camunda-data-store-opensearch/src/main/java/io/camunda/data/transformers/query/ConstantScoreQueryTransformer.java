/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.data.transformers.query;

import io.camunda.data.clients.query.DataStoreConstantScoreQuery;
import io.camunda.data.transformers.OpensearchTransformers;
import org.opensearch.client.opensearch._types.query_dsl.ConstantScoreQuery;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;

public final class ConstantScoreQueryTransformer
    extends QueryVariantTransformer<DataStoreConstantScoreQuery, ConstantScoreQuery> {

  public ConstantScoreQueryTransformer(final OpensearchTransformers transformers) {
    super(transformers);
  }

  @Override
  public ConstantScoreQuery apply(final DataStoreConstantScoreQuery value) {
    final var transformer = getQueryTransformer();
    final var dataStoreQuery = value.query();
    final var query = transformer.apply(dataStoreQuery);

    if (query != null) {
      return QueryBuilders.constantScore().filter(query).build();
    }

    return null;
  }
}
