/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.tasklist.webapp.security.se;

import static io.camunda.tasklist.util.CollectionUtil.map;
import static io.camunda.tasklist.webapp.security.TasklistProfileService.IDENTITY_AUTH_PROFILE;
import static io.camunda.tasklist.webapp.security.TasklistProfileService.SSO_AUTH_PROFILE;

import io.camunda.tasklist.entities.UserEntity;
import io.camunda.tasklist.property.TasklistProperties;
import io.camunda.tasklist.webapp.rest.exception.NotFoundApiException;
import io.camunda.tasklist.webapp.security.se.store.UserStore;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Configuration
@Component
@Profile("!" + SSO_AUTH_PROFILE + " & !" + IDENTITY_AUTH_PROFILE)
public class SearchEngineUserDetailsService implements UserDetailsService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SearchEngineUserDetailsService.class);

  @Autowired private UserStore userStore;

  @Autowired private TasklistProperties tasklistProperties;

  @Bean
  public PasswordEncoder getPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }

  public void initializeUsers() {
    final boolean createSchema =
        TasklistProperties.ELASTIC_SEARCH.equalsIgnoreCase(tasklistProperties.getDatabase())
            ? tasklistProperties.getElasticsearch().isCreateSchema()
            : tasklistProperties.getOpenSearch().isCreateSchema();

    if (createSchema) {
      final String userId = tasklistProperties.getUserId();
      final String displayName = tasklistProperties.getDisplayName();
      final String password = tasklistProperties.getPassword();

      final String readerUserId = tasklistProperties.getReaderUserId();
      final String readerDisplayName = tasklistProperties.getReaderDisplayName();
      final String readerPassword = tasklistProperties.getReaderPassword();

      final String operatorUserId = tasklistProperties.getOperatorUserId();
      final String operatorDisplayName = tasklistProperties.getOperatorDisplayName();
      final String operatorPassword = tasklistProperties.getOperatorPassword();

      final List<String> roles = tasklistProperties.getRoles();
      if (!userExists(userId)) {
        addUserWith(userId, displayName, password, roles);
      }
      if (!userExists(readerUserId)) {
        addUserWith(readerUserId, readerDisplayName, readerPassword, List.of(Role.READER.name()));
      }
      if (!userExists(operatorUserId)) {
        addUserWith(
            operatorUserId, operatorDisplayName, operatorPassword, List.of(Role.OPERATOR.name()));
      }
    }
  }

  private boolean userExists(String userId) {
    try {
      return userStore.getByUserId(userId) != null;
    } catch (Exception t) {
      return false;
    }
  }

  SearchEngineUserDetailsService addUserWith(
      final String userId,
      final String displayName,
      final String password,
      final List<String> roles) {
    LOGGER.info("Create user with userId {}", userId);
    final String passwordEncoded = getPasswordEncoder().encode(password);
    final UserEntity userEntity =
        new UserEntity()
            .setId(userId)
            .setUserId(userId)
            .setDisplayName(displayName)
            .setPassword(passwordEncoded)
            .setRoles(roles);
    userStore.create(userEntity);
    return this;
  }

  @Override
  public User loadUserByUsername(String username) throws UsernameNotFoundException {
    try {
      final UserEntity userEntity = userStore.getByUserId(username);
      return new User(
              userEntity.getUserId(),
              userEntity.getPassword(),
              map(userEntity.getRoles(), Role::fromString))
          .setDisplayName(userEntity.getDisplayName())
          .setRoles(map(userEntity.getRoles(), Role::fromString));
    } catch (NotFoundApiException e) {
      throw new UsernameNotFoundException(
          String.format("User with user id '%s' not found.", username), e);
    }
  }
}
