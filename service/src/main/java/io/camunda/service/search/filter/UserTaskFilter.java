/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.search.filter;

import static io.camunda.util.CollectionUtil.addValuesToList;
import static io.camunda.util.CollectionUtil.collectValues;

import io.camunda.util.ObjectBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final record UserTaskFilter(
    List<Long> userTaskKeys,
    List<String> userTaskDefinitionIds,
    List<String> processNames,
    List<String> assignees,
    List<String> userTaskState,
    List<Long> processInstanceKeys,
    List<Long> processDefinitionKeys,
    List<String> candidateUsers,
    List<String> candidateGroups,
    boolean created,
    boolean completed,
    boolean canceled,
    DateValueFilter creationDateFilter,
    DateValueFilter completionDateFilter,
    DateValueFilter dueDateFilter,
    DateValueFilter followUpDateFilter,
    List<VariableValueFilter> variableFilters,
    List<String> tenantIds)
    implements FilterBase {

  public static final class Builder implements ObjectBuilder<UserTaskFilter> {
    private List<Long> userTaskKeys;
    private List<String> userTaskDefinitionIds;
    private List<String> processNames;
    private List<String> assignees;
    private List<String> userTaskState;
    private List<Long> processInstanceKeys;
    private List<Long> processDefinitionKeys;
    private List<String> candidateUsers;
    private List<String> candidateGroups;
    private boolean created;
    private boolean completed;
    private boolean canceled;
    private DateValueFilter creationDateFilter;
    private DateValueFilter completionDateFilter;
    private DateValueFilter dueDateFilter;
    private DateValueFilter followUpDateFilter;
    private List<VariableValueFilter> variableFilters;
    private List<String> tenantIds;

    public Builder userTaskKeys(final Long value, final Long... values) {
      return userTaskKeys(collectValues(value, values));
    }

    public Builder userTaskKeys(final List<Long> values) {
      userTaskKeys = addValuesToList(userTaskKeys, values);
      return this;
    }

    public Builder userTaskDefinitionIds(final String value, final String... values) {
      return userTaskDefinitionIds(collectValues(value, values));
    }

    public Builder userTaskDefinitionIds(final List<String> values) {
      userTaskDefinitionIds = addValuesToList(userTaskDefinitionIds, values);
      return this;
    }

    public Builder processNames(final String value, final String... values) {
      return processNames(collectValues(value, values));
    }

    public Builder processNames(final List<String> values) {
      processNames = addValuesToList(processNames, values);
      return this;
    }

    public Builder assignees(final String value, final String... values) {
      return assignees(collectValues(value, values));
    }

    public Builder assignees(final List<String> values) {
      assignees = addValuesToList(assignees, values);
      return this;
    }

    public Builder userTaskState(final String value, final String... values) {
      return userTaskState(collectValues(value, values));
    }

    public Builder userTaskState(final List<String> values) {
      userTaskState = addValuesToList(userTaskState, values);
      return this;
    }

    public Builder creationDate(final DateValueFilter value) {
      creationDateFilter = value;
      return this;
    }

    public Builder creationDate(
        final Function<DateValueFilter.Builder, ObjectBuilder<DateValueFilter>> fn) {
      return creationDate(FilterBuilders.dateValue(fn));
    }

    public Builder completionDate(final DateValueFilter value) {
      completionDateFilter = value;
      return this;
    }

    public Builder completionDate(
        final Function<DateValueFilter.Builder, ObjectBuilder<DateValueFilter>> fn) {
      return completionDate(FilterBuilders.dateValue(fn));
    }

    public Builder dueDate(final DateValueFilter value) {
      dueDateFilter = value;
      return this;
    }

    public Builder dueDate(
        final Function<DateValueFilter.Builder, ObjectBuilder<DateValueFilter>> fn) {
      return dueDate(FilterBuilders.dateValue(fn));
    }

    public Builder followUpDate(final DateValueFilter value) {
      followUpDateFilter = value;
      return this;
    }

    public Builder followUpDate(
        final Function<DateValueFilter.Builder, ObjectBuilder<DateValueFilter>> fn) {
      return followUpDate(FilterBuilders.dateValue(fn));
    }

    public Builder variable(final List<VariableValueFilter> values) {
      variableFilters = addValuesToList(variableFilters, values);
      return this;
    }

    public Builder variable(final VariableValueFilter value, final VariableValueFilter... values) {
      return variable(collectValues(value, values));
    }

    public Builder variable(
        final Function<VariableValueFilter.Builder, ObjectBuilder<VariableValueFilter>> fn) {
      return variable(FilterBuilders.variableValue(fn));
    }

    public Builder processInstanceKeys(final Long value, final Long... values) {
      return processInstanceKeys(collectValues(value, values));
    }

    public Builder processInstanceKeys(final List<Long> values) {
      processInstanceKeys = addValuesToList(processInstanceKeys, values);
      return this;
    }

    public Builder processDefinitionKeys(final Long value, final Long... values) {
      return processDefinitionKeys(collectValues(value, values));
    }

    public Builder processDefinitionKeys(final List<Long> values) {
      processDefinitionKeys = addValuesToList(processDefinitionKeys, values);
      return this;
    }

    public Builder candidateUsers(final String value, final String... values) {
      return candidateUsers(collectValues(value, values));
    }

    public Builder candidateUsers(final List<String> values) {
      candidateUsers = addValuesToList(candidateUsers, values);
      return this;
    }

    public Builder candidateGroups(final String value, final String... values) {
      return candidateGroups(collectValues(value, values));
    }

    public Builder candidateGroups(final List<String> values) {
      candidateGroups = addValuesToList(candidateGroups, values);
      return this;
    }

    public Builder tenantIds(final String value, final String... values) {
      return tenantIds(collectValues(value, values));
    }

    public Builder tenantIds(final List<String> values) {
      tenantIds = addValuesToList(tenantIds, values);
      return this;
    }

    public Builder created() {
      created = true;
      return this;
    }

    public Builder completed() {
      completed = true;
      return this;
    }

    public Builder canceled() {
      canceled = true;
      return this;
    }

    @Override
    public UserTaskFilter build() {
      return new UserTaskFilter(
          Objects.requireNonNullElse(userTaskKeys, Collections.emptyList()),
          Objects.requireNonNullElse(userTaskDefinitionIds, Collections.emptyList()),
          Objects.requireNonNullElse(processNames, Collections.emptyList()),
          Objects.requireNonNullElse(assignees, Collections.emptyList()),
          Objects.requireNonNullElse(userTaskState, Collections.emptyList()),
          Objects.requireNonNullElse(processInstanceKeys, Collections.emptyList()),
          Objects.requireNonNullElse(processDefinitionKeys, Collections.emptyList()),
          Objects.requireNonNullElse(candidateUsers, Collections.emptyList()),
          Objects.requireNonNullElse(candidateGroups, Collections.emptyList()),
          created,
          completed,
          canceled,
          creationDateFilter,
          completionDateFilter,
          dueDateFilter,
          followUpDateFilter,
          Objects.requireNonNullElse(variableFilters, Collections.emptyList()),
          Objects.requireNonNullElse(tenantIds, Collections.emptyList()));
    }
  }
}
