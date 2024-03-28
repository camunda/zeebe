/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.exporter.stream;

import io.camunda.zeebe.protocol.record.ValueType;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public final class ExporterMetrics {

  private static final Counter EXPORTER_EVENTS =
      Counter.build()
          .namespace("zeebe")
          .name("exporter_events_total")
          .help("Number of events processed by exporter")
          .labelNames("action", "partition", "valueType")
          .register();

  private static final Gauge LAST_EXPORTED_POSITION =
      Gauge.build()
          .namespace("zeebe")
          .name("exporter_last_exported_position")
          .help("The last exported position by exporter and partition.")
          .labelNames("exporter", "partition")
          .register();

  private static final Gauge LAST_UPDATED_EXPORTED_POSITION =
      Gauge.build()
          .namespace("zeebe")
          .name("exporter_last_updated_exported_position")
          .help("The last exported position which was also updated/committed by the exporter.")
          .labelNames("exporter", "partition")
          .register();

  private static final Gauge EXPORTER_STATE =
      Gauge.build()
          .namespace("zeebe")
          .name("exporter_state")
          .help(
              "Describes the state of the exporter, namely if it is exporting, paused or soft paused.")
          .labelNames("exporter", "partition")
          .register();
  private final String partitionIdLabel;

  private final Gauge.Child exporterState;

  public ExporterMetrics(final int partitionId) {
    partitionIdLabel = String.valueOf(partitionId);
    exporterState = EXPORTER_STATE.labels("exporter", "partition");
  }

  private void event(final String action, final ValueType valueType) {
    EXPORTER_EVENTS.labels(action, partitionIdLabel, valueType.name()).inc();
  }

  public void setExporterActive() {
    exporterState.set(0);
  }

  public void setExporterPaused() {
    exporterState.set(1);
  }

  public void setExporterSoftPaused() {
    exporterState.set(2);
  }

  public void eventExported(final ValueType valueType) {
    event("exported", valueType);
  }

  public void eventSkipped(final ValueType valueType) {
    event("skipped", valueType);
  }

  public void setLastUpdatedExportedPosition(final String exporter, final long position) {
    LAST_UPDATED_EXPORTED_POSITION.labels(exporter, partitionIdLabel).set(position);
  }

  public void setLastExportedPosition(final String exporter, final long position) {
    LAST_EXPORTED_POSITION.labels(exporter, partitionIdLabel).set(position);
  }
}
