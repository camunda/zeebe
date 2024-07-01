/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.logstreams.impl.flowcontrol;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.concurrency.limits.Limit;
import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.Limiter.Listener;
import io.camunda.zeebe.logstreams.impl.LogStreamMetrics;
import io.camunda.zeebe.logstreams.impl.flowcontrol.RequestLimiter.CommandRateLimiterBuilder;
import io.camunda.zeebe.logstreams.impl.log.LogAppendEntryMetadata;
import io.camunda.zeebe.logstreams.log.WriteContext;
import io.camunda.zeebe.logstreams.log.WriteContext.UserCommand;
import io.camunda.zeebe.logstreams.storage.LogStorage.AppendListener;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.scheduler.clock.ActorClock;
import io.camunda.zeebe.util.Either;
import java.time.Duration;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Maintains a view of in-flight entries as they are being appended, written, committed and finally
 * processed.
 *
 * <p>If enabled, a write rate limiter is used to limit the rate of appends to the log storage.
 * Additionally, a request limiter is used to limit the amount of unprocessed user commands to
 * ensure fast response times.
 *
 * <h3>Thread safety</h3>
 *
 * Access patterns:
 *
 * <ol>
 *   <li>Calls to {@link #tryAcquire(WriteContext, List)} from the sequencer, serialized through the
 *       sequencers write lock.
 *   <li>Calls to {@link #onAppend(InFlightEntry, long)} from the sequencer, serialized through the
 *       sequencers write lock.
 *   <li>Calls to {@link #onWrite(long, long)} from the log storage, serialized through the single
 *       raft thread.
 *   <li>Calls to {@link #onCommit(long, long)} from the log storage, serialized through the single
 *       raft thread.
 *   <li>Calls to {@link #onProcessed(long)} from the stream processor, serialized through the
 *       stream processor actor.
 * </ol>
 *
 * The order in which these methods are called is weakly constrained:
 *
 * <ul>
 *   <li>{@link #tryAcquire(WriteContext, List)} and {@link #onAppend(InFlightEntry, long)} are
 *       always called in that order and before any other methods.
 *   <li>{@link #onWrite(long, long)} and {@link #onCommit(long, long)} and {@link
 *       #onProcessed(long)} can be called in any order.
 * </ul>
 *
 * The weak ordering forces us to program quite defensively and carefully choose where and how we
 * modify internal state.
 *
 * <p>The {@link #inFlight} map is only modified in the {@link #onAppend(InFlightEntry, long)}
 * method. All other methods only read from it.
 *
 * <p>A volatile field {@link #lastProcessedPosition} is only modified in {@link #onProcessed(long)}
 * and used in {@link #onAppend(InFlightEntry, long)} to clean up old entries.
 */
@SuppressWarnings("UnstableApiUsage")
public final class FlowControl implements AppendListener {
  private final LogStreamMetrics metrics;
  private final Limiter<Intent> processingLimiter;
  private final RateLimiter writeRateLimiter;
  private final RateMeasurement exportingRate =
      new RateMeasurement(
          ActorClock::currentTimeMillis, Duration.ofMinutes(5), Duration.ofSeconds(10));
  private volatile long lastProcessedPosition = -1;

  private final NavigableMap<Long, InFlightEntry> inFlight = new TreeMap<>();

  public FlowControl(final LogStreamMetrics metrics) {
    this(metrics, StabilizingAIMDLimit.newBuilder().build(), RateLimit.disabled());
  }

  public FlowControl(
      final LogStreamMetrics metrics, final Limit requestLimit, final RateLimit writeRateLimit) {
    this.metrics = metrics;
    processingLimiter =
        requestLimit != null
            ? new CommandRateLimiterBuilder().limit(requestLimit).build(metrics)
            : new NoopLimiter<>();
    writeRateLimiter = writeRateLimit == null ? null : writeRateLimit.limiter();
  }

  /**
   * Tries to acquire a free in-flight spot, applying backpressure as needed.
   *
   * @return An Optional containing a {@link InFlightEntry} if append was accepted, an empty
   *     Optional otherwise.
   */
  public Either<Rejection, InFlightEntry> tryAcquire(
      final WriteContext context, final List<LogAppendEntryMetadata> batchMetadata) {
    final var result = tryAcquireInternal(context, batchMetadata);
    switch (result) {
      case Either.Left<Rejection, InFlightEntry>(final var reason) ->
          metrics.flowControlRejected(context, batchMetadata, reason);
      case Either.Right<Rejection, InFlightEntry>(final var ignored) ->
          metrics.flowControlAccepted(context, batchMetadata);
    }
    return result;
  }

  private Either<Rejection, InFlightEntry> tryAcquireInternal(
      final WriteContext context, final List<LogAppendEntryMetadata> batchMetadata) {
    final Listener requestListener;
    if (context instanceof UserCommand(final var intent)) {
      requestListener = processingLimiter.acquire(intent).orElse(null);
      if (requestListener == null) {
        return Either.left(Rejection.RequestLimitExhausted);
      }
    } else {
      requestListener = null;
    }

    if (writeRateLimiter != null && !writeRateLimiter.tryAcquire(batchMetadata.size())) {
      if (requestListener != null) {
        requestListener.onIgnore();
      }
      return Either.left(Rejection.WriteRateLimitExhausted);
    }

    return Either.right(new InFlightEntry(metrics, batchMetadata, requestListener));
  }

  public void onAppend(final InFlightEntry entry, final long highestPosition) {
    entry.onAppend();
    metrics.increaseInflightAppends();
    final var clearable = inFlight.headMap(lastProcessedPosition, true);
    clearable.forEach((position, inFlightEntry) -> inFlightEntry.cleanup());
    clearable.clear();
    inFlight.put(highestPosition, entry);
  }

  @Override
  public void onWrite(final long index, final long highestPosition) {
    metrics.setLastWrittenPosition(highestPosition);
    final var inFlightEntry = inFlight.get(highestPosition);
    if (inFlightEntry != null) {
      inFlightEntry.onWrite();
    }
  }

  @Override
  public void onCommit(final long index, final long highestPosition) {
    metrics.setLastCommittedPosition(highestPosition);
    metrics.decreaseInflightAppends();
    final var inFlightEntry = inFlight.get(highestPosition);
    if (inFlightEntry != null) {
      inFlightEntry.onCommit();
    }
  }

  public void onProcessed(final long position) {
    final var inFlightEntry = inFlight.get(position);
    if (inFlightEntry != null) {
      inFlightEntry.onProcessed();
    }
    lastProcessedPosition = position;
  }

  public void onExported(final long position) {
    if (position <= 0) {
      return;
    }
    if (exportingRate.observe(position)) {
      metrics.setExportingRate(exportingRate.rate());
    }
  }

  public enum Rejection {
    WriteRateLimitExhausted,
    RequestLimitExhausted
  }
}
