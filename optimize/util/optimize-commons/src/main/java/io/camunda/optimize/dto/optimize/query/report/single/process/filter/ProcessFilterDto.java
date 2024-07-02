/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterDataDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Abstract class that contains a hidden "type" field to distinguish, which filter type the jackson
 * object mapper should transform the object to.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = InstanceStartDateFilterDto.class, name = "instanceStartDate"),
  @JsonSubTypes.Type(value = FlowNodeStartDateFilterDto.class, name = "flowNodeStartDate"),
  @JsonSubTypes.Type(value = FlowNodeEndDateFilterDto.class, name = "flowNodeEndDate"),
  @JsonSubTypes.Type(value = InstanceEndDateFilterDto.class, name = "instanceEndDate"),
  @JsonSubTypes.Type(value = DurationFilterDto.class, name = "processInstanceDuration"),
  @JsonSubTypes.Type(value = VariableFilterDto.class, name = "variable"),
  @JsonSubTypes.Type(value = MultipleVariableFilterDto.class, name = "multipleVariable"),
  @JsonSubTypes.Type(value = ExecutedFlowNodeFilterDto.class, name = "executedFlowNodes"),
  @JsonSubTypes.Type(value = ExecutingFlowNodeFilterDto.class, name = "executingFlowNodes"),
  @JsonSubTypes.Type(value = CanceledFlowNodeFilterDto.class, name = "canceledFlowNodes"),
  @JsonSubTypes.Type(value = RunningInstancesOnlyFilterDto.class, name = "runningInstancesOnly"),
  @JsonSubTypes.Type(
      value = CompletedInstancesOnlyFilterDto.class,
      name = "completedInstancesOnly"),
  @JsonSubTypes.Type(value = CanceledInstancesOnlyFilterDto.class, name = "canceledInstancesOnly"),
  @JsonSubTypes.Type(
      value = NonCanceledInstancesOnlyFilterDto.class,
      name = "nonCanceledInstancesOnly"),
  @JsonSubTypes.Type(
      value = SuspendedInstancesOnlyFilterDto.class,
      name = "suspendedInstancesOnly"),
  @JsonSubTypes.Type(
      value = NonSuspendedInstancesOnlyFilterDto.class,
      name = "nonSuspendedInstancesOnly"),
  @JsonSubTypes.Type(value = FlowNodeDurationFilterDto.class, name = "flowNodeDuration"),
  @JsonSubTypes.Type(value = AssigneeFilterDto.class, name = "assignee"),
  @JsonSubTypes.Type(value = CandidateGroupFilterDto.class, name = "candidateGroup"),
  @JsonSubTypes.Type(value = OpenIncidentFilterDto.class, name = "includesOpenIncident"),
  @JsonSubTypes.Type(value = DeletedIncidentFilterDto.class, name = "includesClosedIncident"),
  @JsonSubTypes.Type(value = ResolvedIncidentFilterDto.class, name = "includesResolvedIncident"),
  @JsonSubTypes.Type(value = NoIncidentFilterDto.class, name = "doesNotIncludeIncident"),
  @JsonSubTypes.Type(value = RunningFlowNodesOnlyFilterDto.class, name = "runningFlowNodesOnly"),
  @JsonSubTypes.Type(
      value = CompletedFlowNodesOnlyFilterDto.class,
      name = "completedFlowNodesOnly"),
  @JsonSubTypes.Type(value = CanceledFlowNodesOnlyFilterDto.class, name = "canceledFlowNodesOnly"),
  @JsonSubTypes.Type(
      value = CompletedOrCanceledFlowNodesOnlyFilterDto.class,
      name = "completedOrCanceledFlowNodesOnly")
})
@Data
@NoArgsConstructor
@FieldNameConstants
public abstract class ProcessFilterDto<DATA extends FilterDataDto> {

  protected DATA data;
  @NotNull protected FilterApplicationLevel filterLevel;

  @NotEmpty protected List<String> appliedTo = List.of(ReportConstants.APPLIED_TO_ALL_DEFINITIONS);

  protected ProcessFilterDto(final DATA data, FilterApplicationLevel filterLevel) {
    this.data = data;
    setFilterLevel(filterLevel);
  }

  public abstract List<FilterApplicationLevel> validApplicationLevels();

  @Override
  public String toString() {
    return "ProcessFilter=" + getClass().getSimpleName();
  }
}
