/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.data.transformers.query;

import io.camunda.data.clients.query.DataStoreTermsQuery;
import io.camunda.data.clients.types.DataStoreTypedValue;
import io.camunda.data.transformers.OpensearchTransformers;
import java.util.List;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;

public final class TermsQueryTransformer
    extends QueryVariantTransformer<DataStoreTermsQuery, TermsQuery> {

  public TermsQueryTransformer(final OpensearchTransformers transformers) {
    super(transformers);
  }

  @Override
  public TermsQuery apply(final DataStoreTermsQuery value) {
    final var field = value.field();
    final var values = value.values();
    final var termsQueryField = of(values);

    return QueryBuilders.terms().field(field).terms(termsQueryField).build();
  }

  private <T> TermsQueryField of(final List<DataStoreTypedValue> values) {
    final var transformer = getFieldValueTransformer();
    final var fieldValues = values.stream().map(transformer::apply).toList();
    return TermsQueryField.of(f -> f.value(fieldValues));
  }
}
