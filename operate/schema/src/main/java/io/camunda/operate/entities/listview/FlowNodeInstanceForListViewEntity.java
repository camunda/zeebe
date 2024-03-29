/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.operate.entities.listview;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.operate.entities.FlowNodeState;
import io.camunda.operate.entities.FlowNodeType;
import io.camunda.operate.entities.OperateZeebeEntity;
import io.camunda.operate.schema.templates.ListViewTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FlowNodeInstanceForListViewEntity
    extends OperateZeebeEntity<FlowNodeInstanceForListViewEntity> {

  private Long processInstanceKey;
  private String activityId;
  private FlowNodeState activityState;
  private FlowNodeType activityType;
  @Deprecated @JsonIgnore private List<Long> incidentKeys = new ArrayList<>();
  private String errorMessage;
  private boolean incident;
  private boolean jobFailedWithRetriesLeft = false;

  private String tenantId;

  @Deprecated @JsonIgnore private boolean pendingIncident;

  private ListViewJoinRelation joinRelation =
      new ListViewJoinRelation(ListViewTemplate.ACTIVITIES_JOIN_RELATION);

  @JsonIgnore private Long startTime;
  @JsonIgnore private Long endTime;

  public Long getProcessInstanceKey() {
    return processInstanceKey;
  }

  public void setProcessInstanceKey(Long processInstanceKey) {
    this.processInstanceKey = processInstanceKey;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public FlowNodeState getActivityState() {
    return activityState;
  }

  public void setActivityState(FlowNodeState activityState) {
    this.activityState = activityState;
  }

  public FlowNodeType getActivityType() {
    return activityType;
  }

  public void setActivityType(FlowNodeType activityType) {
    this.activityType = activityType;
  }

  public List<Long> getIncidentKeys() {
    return incidentKeys;
  }

  public FlowNodeInstanceForListViewEntity setIncidentKeys(List<Long> incidentKeys) {
    this.incidentKeys = incidentKeys;
    return this;
  }

  public FlowNodeInstanceForListViewEntity addIncidentKey(Long incidentKey) {
    this.incidentKeys.add(incidentKey);
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public boolean isIncident() {
    return incident;
  }

  public FlowNodeInstanceForListViewEntity setIncident(final boolean incident) {
    this.incident = incident;
    return this;
  }

  public String getTenantId() {
    return tenantId;
  }

  public FlowNodeInstanceForListViewEntity setTenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public boolean isPendingIncident() {
    return pendingIncident;
  }

  public FlowNodeInstanceForListViewEntity setPendingIncident(boolean pendingIncident) {
    this.pendingIncident = pendingIncident;
    return this;
  }

  public ListViewJoinRelation getJoinRelation() {
    return joinRelation;
  }

  public void setJoinRelation(ListViewJoinRelation joinRelation) {
    this.joinRelation = joinRelation;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public boolean isJobFailedWithRetriesLeft() {
    return jobFailedWithRetriesLeft;
  }

  public FlowNodeInstanceForListViewEntity setJobFailedWithRetriesLeft(
      boolean jobFailedWithRetriesLeft) {
    this.jobFailedWithRetriesLeft = jobFailedWithRetriesLeft;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final FlowNodeInstanceForListViewEntity that = (FlowNodeInstanceForListViewEntity) o;
    return incident == that.incident
        && Objects.equals(processInstanceKey, that.processInstanceKey)
        && Objects.equals(activityId, that.activityId)
        && activityState == that.activityState
        && activityType == that.activityType
        && Objects.equals(errorMessage, that.errorMessage)
        && Objects.equals(joinRelation, that.joinRelation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        processInstanceKey,
        activityId,
        activityState,
        activityType,
        errorMessage,
        incident,
        joinRelation);
  }
}
