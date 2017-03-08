package org.camunda.optimize.dto.optimize;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Askar Akhmerov
 */
public class EventDto implements Serializable,OptimizeDto {

  private String id;
  private String activityId;
  private String state;
  private String activityInstanceId;
  private Date timestamp;
  private String processDefinitionKey;
  private String processDefinitionId;
  private String processInstanceId;
  private Date startDate;
  private Date endDate;
  private Date processInstanceStartDate;
  private Date processInstanceEndDate;
  private Long durationInMs;
  private String activityType;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  public void setActivityInstanceId(String activityInstanceId) {
    this.activityInstanceId = activityInstanceId;
  }


  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public Date getProcessInstanceStartDate() {
    return processInstanceStartDate;
  }

  public void setProcessInstanceStartDate(Date processInstanceStartDate) {
    this.processInstanceStartDate = processInstanceStartDate;
  }

  public Date getProcessInstanceEndDate() {
    return processInstanceEndDate;
  }

  public void setProcessInstanceEndDate(Date processInstanceEndDate) {
    this.processInstanceEndDate = processInstanceEndDate;
  }

  public Long getDurationInMs() {
    return durationInMs;
  }

  public void setDurationInMs(Long durationInMs) {
    this.durationInMs = durationInMs;
  }

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }
}
