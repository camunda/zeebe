/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;

public interface QueryFilterEnhancer<T> {
  void addFilterToQuery(BoolQueryBuilder query, List<T> filter, FilterContext filterContext);
}
