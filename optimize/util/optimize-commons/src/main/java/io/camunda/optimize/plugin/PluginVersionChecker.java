/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.plugin;

import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.metadata.Version;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginVersionChecker {

  static final String OPTIMIZE_VERSION_KEY = "optimize.version";
  static final String OPTIMIZE_VERSION_FILE_NAME = "plugin.version";

  public static void validatePluginVersion(PluginClassLoader pluginClassLoader) {
    if (pluginClassLoader == null) {
      throw new IllegalArgumentException("The plugin classloader cannot be null.");
    }

    String pluginVersion =
        extractOptimizeVersion(pluginClassLoader)
            .orElseThrow(
                () ->
                    new OptimizeRuntimeException(
                        buildMissingPluginVersionMessage(Version.VERSION)));

    if (!isValidPluginVersion(pluginVersion)) {
      throw new OptimizeRuntimeException(
          buildUnsupportedPluginVersionMessage(pluginVersion, Version.VERSION));
    }
  }

  private static Optional<String> extractOptimizeVersion(
      final PluginClassLoader pluginClassLoader) {
    Properties property = new Properties();

    try (InputStream resourceAsStream =
        pluginClassLoader.getPluginResourceAsStream(OPTIMIZE_VERSION_FILE_NAME)) {
      if (resourceAsStream != null) {
        property.load(resourceAsStream);
      }
    } catch (IOException e) {
      log.error("Exception during opening plugin resource stream!");
    }

    return Optional.ofNullable(property.getProperty(OPTIMIZE_VERSION_KEY));
  }

  private static boolean isValidPluginVersion(String pluginVersion) {
    if (pluginVersion == null) {
      return false;
    }
    final String optimizeMinorAndMajorVersion = Version.getMajorAndMinor(Version.VERSION);
    return pluginVersion.startsWith(optimizeMinorAndMajorVersion);
  }

  public static String buildMissingPluginVersionMessage(String optimizeVersion) {
    StringBuilder message = new StringBuilder();
    message.append("There is a plugin with a missing Optimize version. ");
    message.append(
        "This either means that the plugin was built with an old Optimize version, or not built as a fat jar! ");
    message.append(getPluginNotSupportedMessage(optimizeVersion));
    message.append(
        "Please upgrade your plugin to the used Optimize version and build it as a fat jar!");

    return message.toString();
  }

  public static String buildUnsupportedPluginVersionMessage(
      String pluginVersion, String optimizeVersion) {
    StringBuilder message = new StringBuilder();
    message
        .append("There is a plugin that was built with Optimize version ")
        .append(pluginVersion)
        .append(". ");
    message.append(getPluginNotSupportedMessage(optimizeVersion));
    message.append("Please upgrade your plugin to the used Optimize version!");
    return message.toString();
  }

  private static String getPluginNotSupportedMessage(final String optimizeVersion) {
    return String.format("This plugin is not supported by Optimize version %s. ", optimizeVersion);
  }
}
