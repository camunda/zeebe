/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.collection;

import io.camunda.optimize.dto.optimize.GroupDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.IdentityWithMetadataResponseDto;
import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.UserDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldNameConstants(asEnum = true)
public class CollectionRoleResponseDto implements Comparable<CollectionRoleResponseDto> {

  private static final String ID_SEGMENT_SEPARATOR = ":";

  @Setter(value = AccessLevel.PROTECTED)
  private String id;

  private IdentityWithMetadataResponseDto identity;
  private RoleType role;

  public CollectionRoleResponseDto(CollectionRoleResponseDto oldRole) {
    if (oldRole.getIdentity().getType().equals(IdentityType.USER)) {
      UserDto oldUserDto = (UserDto) oldRole.getIdentity();
      this.identity =
          new UserDto(
              oldUserDto.getId(),
              oldUserDto.getFirstName(),
              oldUserDto.getLastName(),
              oldUserDto.getEmail());
    } else {
      GroupDto oldGroupDto = (GroupDto) oldRole.getIdentity();
      this.identity =
          new GroupDto(oldGroupDto.getId(), oldGroupDto.getName(), oldGroupDto.getMemberCount());
    }

    this.role = oldRole.role;
    this.id = convertIdentityToRoleId(this.identity);
  }

  public CollectionRoleResponseDto(IdentityWithMetadataResponseDto identity, RoleType role) {
    this.identity = identity;
    this.id = convertIdentityToRoleId(this.identity);
    this.role = role;
  }

  @Override
  public int compareTo(final CollectionRoleResponseDto other) {
    if (this.identity instanceof UserDto && other.getIdentity() instanceof GroupDto) {
      return 1;
    } else if (this.identity instanceof GroupDto && other.getIdentity() instanceof UserDto) {
      return -1;
    } else {
      return StringUtils.compareIgnoreCase(this.identity.getName(), other.getIdentity().getName());
    }
  }

  private String convertIdentityToRoleId(final IdentityWithMetadataResponseDto identity) {
    return identity.getType().name() + ID_SEGMENT_SEPARATOR + identity.getId();
  }

  public static <T extends IdentityWithMetadataResponseDto> CollectionRoleResponseDto from(
      final CollectionRoleRequestDto roleDto, T identityWithMetaData) {
    return new CollectionRoleResponseDto(identityWithMetaData, roleDto.getRole());
  }
}
