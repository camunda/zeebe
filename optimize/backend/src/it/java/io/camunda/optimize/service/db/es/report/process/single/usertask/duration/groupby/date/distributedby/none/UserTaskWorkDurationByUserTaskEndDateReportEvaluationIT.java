/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.date.distributedby.none;

import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;

public class UserTaskWorkDurationByUserTaskEndDateReportEvaluationIT
    extends UserTaskDurationByUserTaskEndDateReportEvaluationIT {

  @Override
  protected UserTaskDurationTime getUserTaskDurationTime() {
    return UserTaskDurationTime.WORK;
  }

  @Override
  protected void changeDuration(
      final ProcessInstanceEngineDto processInstanceDto, final Double durationInMs) {
    changeUserTaskWorkDuration(processInstanceDto, durationInMs);
  }

  @Override
  protected void changeDuration(
      final ProcessInstanceEngineDto processInstanceDto,
      final String modelElementId,
      final Double durationInMs) {
    changeUserTaskWorkDuration(processInstanceDto, modelElementId, durationInMs);
  }
}
