/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.plugin;

import org.camunda.optimize.plugin.security.authentication.AuthenticationExtractor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthenticationExtractorProvider extends PluginProvider<AuthenticationExtractor> {

  @Override
  protected Class<AuthenticationExtractor> getPluginClass() {
    return AuthenticationExtractor.class;
  }

  @Override
  protected List<String> getBasePackages() {
    return configurationService.getAuthenticationExtractorPluginBasePackages();
  }

}
