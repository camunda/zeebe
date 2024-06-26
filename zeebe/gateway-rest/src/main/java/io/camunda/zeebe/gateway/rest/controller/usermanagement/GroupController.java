/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.controller.usermanagement;

import io.camunda.identity.automation.usermanagement.CamundaGroup;
import io.camunda.identity.automation.usermanagement.service.GroupService;
import io.camunda.zeebe.gateway.protocol.rest.CamundaGroupRequest;
import io.camunda.zeebe.gateway.protocol.rest.CamundaGroupResponse;
import io.camunda.zeebe.gateway.protocol.rest.GroupSearchResponse;
import io.camunda.zeebe.gateway.protocol.rest.SearchQueryRequest;
import io.camunda.zeebe.gateway.rest.RestErrorMapper;
import io.camunda.zeebe.gateway.rest.controller.ZeebeRestController;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ZeebeRestController
@RequestMapping("/v2/groups")
public class GroupController {
  private final GroupService groupService;

  public GroupController(final GroupService groupService) {
    this.groupService = groupService;
  }

  @PostMapping(
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createGroup(@RequestBody final CamundaGroupRequest groupRequest) {
    try {
      final CamundaGroupResponse camundaGroupResponse =
          mapToGroupResponse(groupService.createGroup(mapToGroup(groupRequest)));
      return new ResponseEntity<>(camundaGroupResponse, HttpStatus.CREATED);
    } catch (final Exception e) {
      return RestErrorMapper.mapUserManagementExceptionsToResponse(e);
    }
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Object> deleteGroup(@PathVariable(name = "id") final Long groupId) {
    try {
      groupService.deleteGroupById(groupId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (final Exception e) {
      return RestErrorMapper.mapUserManagementExceptionsToResponse(e);
    }
  }

  @GetMapping(
      path = "/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
  public ResponseEntity<Object> findGroupById(@PathVariable(name = "id") final Long groupId) {
    try {
      final CamundaGroupResponse camundaGroupResponse =
          mapToGroupResponse(groupService.findGroupById(groupId));
      return new ResponseEntity<>(camundaGroupResponse, HttpStatus.OK);
    } catch (final Exception e) {
      return RestErrorMapper.mapUserManagementExceptionsToResponse(e);
    }
  }

  @PostMapping(
      path = "/search",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> findAllGroups(
      @RequestBody(required = false) final SearchQueryRequest searchQueryRequest) {
    try {
      final GroupSearchResponse groupSearchResponse = new GroupSearchResponse();
      final List<CamundaGroupResponse> allGroupResponses =
          groupService.findAllGroups().stream().map(this::mapToGroupResponse).toList();
      groupSearchResponse.setItems(allGroupResponses);

      return new ResponseEntity<>(groupSearchResponse, HttpStatus.OK);
    } catch (final Exception e) {
      return RestErrorMapper.mapUserManagementExceptionsToResponse(e);
    }
  }

  @PutMapping(
      path = "/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> updateGroup(
      @PathVariable(name = "id") final Long groupId,
      @RequestBody final CamundaGroupRequest groupRequest) {
    try {
      final CamundaGroupResponse camundaGroupResponse =
          mapToGroupResponse(groupService.updateGroup(groupId, mapToGroup(groupRequest)));
      return new ResponseEntity<>(camundaGroupResponse, HttpStatus.OK);
    } catch (final Exception e) {
      return RestErrorMapper.mapUserManagementExceptionsToResponse(e);
    }
  }

  private CamundaGroup mapToGroup(final CamundaGroupRequest groupRequest) {
    return new CamundaGroup(groupRequest.getId(), groupRequest.getName());
  }

  private CamundaGroupResponse mapToGroupResponse(final CamundaGroup group) {
    final CamundaGroupResponse camundaGroupResponse = new CamundaGroupResponse();
    camundaGroupResponse.setId(group.id());
    camundaGroupResponse.setName(group.name());
    return camundaGroupResponse;
  }
}
