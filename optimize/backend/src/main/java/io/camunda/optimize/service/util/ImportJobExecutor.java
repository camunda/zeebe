/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ImportJobExecutor {

  protected static final Logger logger = LoggerFactory.getLogger(ImportJobExecutor.class);
  private final String name;
  private ThreadPoolExecutor importExecutor;

  protected ImportJobExecutor(final String name) {
    this.name = name;
  }

  public void shutdown() {
    stopExecutingImportJobs();
  }

  public boolean isActive() {
    return importExecutor.getActiveCount() > 0 || !importExecutor.getQueue().isEmpty();
  }

  public void executeImportJob(final Runnable dbImportJob) {
    logger.debug(
        "{}: Currently active [{}] jobs and [{}] in queue of job type [{}]",
        getClass().getSimpleName(),
        importExecutor.getActiveCount(),
        importExecutor.getQueue().size(),
        dbImportJob.getClass().getSimpleName());
    importExecutor.execute(dbImportJob);
  }

  public void startExecutingImportJobs() {
    if (importExecutor == null || importExecutor.isShutdown()) {
      final BlockingQueue<Runnable> importJobsQueue = new ArrayBlockingQueue<>(getMaxQueueSize());
      importExecutor =
          new ThreadPoolExecutor(
              getExecutorThreadCount(),
              getExecutorThreadCount(),
              Long.MAX_VALUE,
              TimeUnit.DAYS,
              importJobsQueue,
              new ThreadFactoryBuilder()
                  .setNameFormat("ImportJobExecutor-pool-" + name + "-%d")
                  .build(),
              new BlockCallerUntilExecutorHasCapacity());
    }
  }

  /** Number of threads that should be used in the thread pool executor. */
  protected abstract int getExecutorThreadCount();

  /** Number of jobs that should be able to accumulate until new submission is blocked. */
  protected abstract int getMaxQueueSize();

  public void stopExecutingImportJobs() {
    // Ask the thread pool to finish and exit
    importExecutor.shutdownNow();

    // Waits for 1 minute to finish all currently executing jobs
    try {
      final boolean timeElapsedBeforeTermination =
          !importExecutor.awaitTermination(60L, TimeUnit.SECONDS);
      if (timeElapsedBeforeTermination) {
        logger.warn(
            "{}: Timeout during shutdown of import job executor! "
                + "The current running jobs could not end within 60 seconds after shutdown operation.",
            getClass().getSimpleName());
      }
    } catch (final InterruptedException e) {
      logger.error(
          "{}: Interrupted while shutting down the import job executor!",
          getClass().getSimpleName(),
          e);
    }
  }

  private class BlockCallerUntilExecutorHasCapacity implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
      // this will block if the queue is full
      if (!executor.isShutdown()) {
        try {
          logger.debug(
              "{}: Max queue capacity is reached and, thus, can't schedule any new jobs. "
                  + "Caller needs to wait until there is new free spot. Job class [{}].",
              super.getClass().getSimpleName(),
              runnable.getClass().getSimpleName());
          executor.getQueue().put(runnable);
          logger.debug(
              "{}: Added job to queue. Caller can continue working on his tasks.",
              super.getClass().getSimpleName());
        } catch (final InterruptedException e) {
          logger.error(
              "{}: Interrupted while waiting to submit a new job to the job executor!",
              getClass().getSimpleName(),
              e);
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
