/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.test.util;

import io.camunda.zeebe.util.Environment;
import io.camunda.zeebe.util.Loggers;
import io.camunda.zeebe.util.ReflectUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * This class mimics the Spring Boot configuration mechanism. It reads configuration from a YAML
 * file or input stream and then overlays a map of environment settings, that can be passed in as
 * argument. It does not consider the actual system environment settings. Instead, the test can
 * specify the environment settings it wants to test through an {@code Environment} object. <br>
 * There are several caveats though:
 *
 * <ul>
 *   <li>If using the file based interface, the file must be available on the classpath
 *   <li>It is assumed that the created types have public no arg constructors
 *   <li>This implementation does not support relaxed binding (it was tried during development, but
 *       in the end the rules for relaxed binding didn't match up with the ones used by Spring Boot
 *       proper. This lead to confusion and eventually the relaxed naming support was dropped)
 *   <li>This class does not support the full feature set of Spring Boot configuration. Among others
 *       it does not support profiles or overlaying configuration from multiple files
 * </ul>
 */
public final class TestConfigurationFactory {

  public static final Logger LOG = Loggers.CONFIG_LOGGER;

  /**
   * Reads the configuration file from the class path and binds it to an object
   *
   * @param environment environment to simulate environment variables that can be overlayed; may be
   *     {@code} null
   * @param prefix the top level element in the configuration that should be mapped to the object
   * @param fileName filename of the configuration file; must be available on the classpath; must
   *     not be {@code null}
   * @param type type of object to be created; it is assumed that this object has a public no arg
   *     constructor; must not be {@code null}
   */
  public <T> T create(
      final Environment environment,
      final String prefix,
      final String fileName,
      final Class<T> type) {
    return create(environment, prefix, fileName, type, ReflectUtil.newInstance(type));
  }

  /**
   * Reads the configuration file from the class path and binds it to an object
   *
   * @param environment environment to simulate environment variables that can be overlayed; may be
   *     {@code} null
   * @param prefix the top level element in the configuration that should be mapped to the object
   * @param fileName filename of the configuration file; must be available on the classpath; must
   *     not be {@code null}
   * @param type type of object to be created; it is assumed that this object has a public no arg
   *     constructor; must not be {@code null}
   * @param instance instance which will be populated with the configuration
   */
  public <T> T create(
      final Environment environment,
      final String prefix,
      final String fileName,
      final Class<T> type,
      final T instance) {
    LOG.debug("Reading configuration for {} from file {}", type, fileName);

    try (final InputStream inputStream = new ClassPathResource(fileName).getInputStream()) {
      return create(environment, prefix, inputStream, type, instance);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <T> T create(
      final Environment environment,
      final String prefix,
      final InputStream inputStream,
      final Class<T> type,
      final T instance) {
    LOG.debug("Reading configuration for {} from input stream", type);

    final Map<String, Object> propertiesFromEnvironment = convertEnvironmentIntoMap(environment);
    final Properties propertiesFromFile = loadYamlProperties(inputStream);

    final MutablePropertySources propertySources = new MutablePropertySources();

    propertySources.addLast(
        new MapPropertySource("environment properties strict", propertiesFromEnvironment));
    propertySources.addLast(
        new PropertiesPropertySource("properties from file", propertiesFromFile));

    final Iterable<ConfigurationPropertySource> configPropertySource =
        ConfigurationPropertySources.from(propertySources);

    final BindResult<T> bindResult =
        new Binder(configPropertySource)
            .bind(
                prefix,
                Bindable.of(type),
                new BindHandler() {
                  @Override
                  public Object onCreate(
                      final ConfigurationPropertyName name,
                      final Bindable<?> target,
                      final BindContext context,
                      final Object result) {
                    return instance;
                  }
                });

    if (!bindResult.isBound()) {
      LOG.warn(
          "No binding result parsing the configuration. This is normal if the configuration is empty."
              + " Otherwise it is a configuration or programming error.");
      return instance;
    } else {
      return bindResult.get();
    }
  }

  private Properties loadYamlProperties(final InputStream inputStream) {
    final Resource resource = new InputStreamResource(inputStream);
    final YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
    factoryBean.setResources(resource);
    return factoryBean.getObject();
  }

  private Map<String, Object> convertEnvironmentIntoMap(final Environment environment) {
    final Map<String, Object> result = new HashMap<>();

    if (environment != null) {
      final Set<String> propertyKeys = environment.getPropertyKeys();
      for (final String propertyKey : propertyKeys) {
        result.put(propertyKey, environment.get(propertyKey).orElse(null));
      }
    }

    return result;
  }
}
