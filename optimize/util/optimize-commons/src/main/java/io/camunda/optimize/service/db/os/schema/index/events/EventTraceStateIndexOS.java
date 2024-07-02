/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.schema.index.events;

import io.camunda.optimize.service.db.os.OptimizeOpenSearchUtil;
import io.camunda.optimize.service.db.schema.index.events.EventTraceStateIndex;
import org.opensearch.client.opensearch.indices.IndexSettings;

public class EventTraceStateIndexOS extends EventTraceStateIndex<IndexSettings.Builder> {

  public EventTraceStateIndexOS(final String indexKey) {
    super(indexKey);
  }

  @Override
  public IndexSettings.Builder addStaticSetting(
      final String key, final int value, final IndexSettings.Builder contentBuilder) {
    return OptimizeOpenSearchUtil.addStaticSetting(key, value, contentBuilder);
  }
}
