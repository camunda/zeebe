/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.protocol.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.protocol.util.ProtocolTypeMapping.Mapping;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;
import org.agrona.collections.MutableReference;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;

@Execution(ExecutionMode.CONCURRENT)
final class ProtocolTypeMappingTest {
  @ParameterizedTest(name = "{0}")
  @MethodSource("protocolClassProvider")
  void shouldMapProtocolClass(
      @SuppressWarnings("unused") final String testName, final Class<?> protocolClass) {
    assertTypeMappingForAbstractType(protocolClass);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("valueClassProvider")
  void shouldMapEveryKnownValueClass(
      @SuppressWarnings("unused") final String testName, final Class<?> valueClass) {
    assertTypeMappingForAbstractType(valueClass);
  }

  /**
   * Asserts that there is a type mapping for the given protocol class, that its abstract class is
   * the given protocol class, its concrete class a concrete implementation of the protocol class,
   * and that it has a builder which is an inner class of the concrete type.
   */
  private void assertTypeMappingForAbstractType(final Class<?> abstractType) {
    final Mapping<?> mapping = findTypeMappingForAbstractType(abstractType);
    assertThat(mapping)
        .as(
            "protocol abstract type '%s' should have a concrete immutable type"
                + " mapping; verify that an equivalent Immutable* class was generated for"
                + " it, and if so, that the ProtocolTypeMapping can correctly find it",
            abstractType)
        .isNotNull();
    assertTypeMappingConcrete(abstractType, mapping);
    assertTypeMappingBuilder(mapping);
  }

  private void assertTypeMappingConcrete(final Class<?> abstractType, final Mapping<?> mapping) {
    assertThat(abstractType)
        .as(
            "type mapping for the protocol class should map itself as an abstract "
                + "type to a concrete implementation")
        .isEqualTo(mapping.getAbstractClass())
        .isAssignableFrom(mapping.getConcreteClass());
  }

  private void assertTypeMappingBuilder(final Mapping<?> mapping) {
    assertThat(mapping.getBuilderClass())
        .as(
            "a builder class should have been assigned to the type mapping for '%s'",
            mapping.getAbstractClass())
        .isNotNull();

    final Optional<Method> buildMethod =
        ReflectionUtils.findMethod(mapping.getBuilderClass(), "build");
    assertThat(buildMethod)
        .as("there should be a no-args build method on the builder class")
        .isPresent()
        .get(InstanceOfAssertFactories.type(Method.class))
        .extracting(Method::getReturnType)
        .as("the build method should return a value of the concrete type")
        .isEqualTo(mapping.getConcreteClass());
  }

  private Mapping<?> findTypeMappingForAbstractType(final Class<?> protocolClass) {
    final MutableReference<Mapping<?>> typeMapping = new MutableReference<>();
    ProtocolTypeMapping.forEach(
        mapping -> {
          if (protocolClass.equals(mapping.getAbstractClass())) {
            typeMapping.set(mapping);
          }
        });

    return typeMapping.get();
  }

  private static Stream<Arguments> protocolClassProvider() {
    return ProtocolTypeMapping.findProtocolTypes().loadClasses().stream()
        .map(protocolClass -> Arguments.of(protocolClass.getName(), protocolClass));
  }

  private static Stream<Arguments> valueClassProvider() {
    return ValueTypeMapping.getAcceptedValueTypes().stream()
        .map(
            valueType ->
                Arguments.of(
                    valueType.name(), ValueTypeMapping.getTypeInfo(valueType).getValueClass()));
  }
}
