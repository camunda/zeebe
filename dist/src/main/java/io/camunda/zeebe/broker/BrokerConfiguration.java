/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker;

import io.camunda.zeebe.broker.WorkingDirectoryConfiguration.WorkingDirectory;
import io.camunda.zeebe.broker.system.configuration.BrokerCfg;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 * Will provide any {@link BrokerCfg} for auto-wiring, guaranteeing they are always initialized.
 *
 * <p>In order to properly initialize the configuration, there's an initial uninitialized bean
 * created on which Spring will bind all the properties. Then, a second bean is created from it
 * which is initialized. The second bean is annotated with {@link Primary}, such that any
 * non-qualified injection will always prefer it.
 */
@Configuration(proxyBeanMethods = false)
public final class BrokerConfiguration {
  @Bean
  @ConfigurationProperties(prefix = "zeebe.broker")
  @ApplicationScope(proxyMode = ScopedProxyMode.NO)
  public BrokerCfg brokerConfig(final WorkingDirectory workingDirectory) {
    return new BrokerCfg(workingDirectory.path().toString());
  }
}
