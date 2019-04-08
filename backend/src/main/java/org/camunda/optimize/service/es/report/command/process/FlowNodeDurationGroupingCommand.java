/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.process;

import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.service.es.report.command.CommandContext;
import org.camunda.optimize.service.es.report.result.process.SingleProcessMapDurationReportResult;

public abstract class FlowNodeDurationGroupingCommand extends ProcessReportCommand<SingleProcessMapDurationReportResult> {

  @Override
  protected SingleProcessMapDurationReportResult filterResultData(final CommandContext<SingleProcessReportDefinitionDto> commandContext,
                                                                  final SingleProcessMapDurationReportResult evaluationResult) {
    return LatestFlowNodesOnlyFilter.filterResultData(commandContext, evaluationResult);
  }

}
