/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.util;

public class BackoffIdleStrategy {

  private static final int NOT_IDLE = 0;
  private static final int IDLE = 1;

  private final long baseIdleTime;
  private final float idleIncreaseFactor;
  private final long maxIdleTime;
  private final int maxIdles;

  private int state = NOT_IDLE;
  private int idles;

  public BackoffIdleStrategy(
      final long baseIdleTime, final float idleIncreaseFactor, final long maxIdleTime) {
    this.baseIdleTime = baseIdleTime;
    this.idleIncreaseFactor = idleIncreaseFactor;
    this.maxIdleTime = maxIdleTime;
    this.maxIdles = ((int) log(idleIncreaseFactor, (double) maxIdleTime / baseIdleTime)) + 1;
  }

  public void idle() {
    if (state == NOT_IDLE) {
      state = IDLE;
      idles = 1;
    } else {
      idles++;
      idles = Math.min(idles, maxIdles);
    }
  }

  public void reset() {
    idles = 0;
    state = NOT_IDLE;
  }

  public long idleTime() {
    final long idleTime;

    if (state == NOT_IDLE) {
      idleTime = 0;
    } else {
      final var t = (long) (baseIdleTime * Math.pow(idleIncreaseFactor, idles - 1.0));
      idleTime = Math.min(maxIdleTime, t);
    }

    return idleTime;
  }

  private double log(double base, double value) {
    return Math.log10(value) / Math.log10(base);
  }
}
