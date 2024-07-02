/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.usertask.frequency.groupby.duration.distributedby.usertask;
//
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import java.time.OffsetDateTime;
//
// public class UserTaskFrequencyByUserTaskTotalDurationByUserTaskIT
//     extends AbstractUserTaskFrequencyByUserTaskDurationByUserTaskIT {
//
//   @Override
//   protected ProcessInstanceEngineDto startProcessInstanceCompleteTaskAndModifyDuration(
//       final String definitionId, final Number durationInMillis) {
//     final ProcessInstanceEngineDto processInstance =
//         engineIntegrationExtension.startProcessInstance(definitionId);
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstance.getId());
//     engineDatabaseExtension.changeAllFlowNodeTotalDurations(
//         processInstance.getId(), durationInMillis);
//     return processInstance;
//   }
//
//   @Override
//   protected void changeRunningInstanceReferenceDate(
//       final ProcessInstanceEngineDto runningProcessInstance, final OffsetDateTime startTime) {
//     engineDatabaseExtension.changeFlowNodeStartDate(
//         runningProcessInstance.getId(), USER_TASK_1, startTime);
//   }
//
//   @Override
//   protected UserTaskDurationTime getUserTaskDurationTime() {
//     return UserTaskDurationTime.TOTAL;
//   }
// }
