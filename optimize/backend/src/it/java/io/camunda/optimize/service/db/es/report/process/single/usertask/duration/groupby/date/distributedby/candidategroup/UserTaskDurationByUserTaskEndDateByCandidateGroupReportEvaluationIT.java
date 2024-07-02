/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.date.distributedby.candidategroup;

import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.service.util.ProcessReportDataType;

public abstract class UserTaskDurationByUserTaskEndDateByCandidateGroupReportEvaluationIT
    extends UserTaskDurationByUserTaskDateByCandidateGroupReportEvaluationIT {

  @Override
  protected ProcessReportDataType getReportDataType() {
    return ProcessReportDataType.USER_TASK_DUR_GROUP_BY_USER_TASK_END_DATE_BY_CANDIDATE_GROUP;
  }

  @Override
  protected ProcessGroupByType getGroupByType() {
    return ProcessGroupByType.END_DATE;
  }
}
