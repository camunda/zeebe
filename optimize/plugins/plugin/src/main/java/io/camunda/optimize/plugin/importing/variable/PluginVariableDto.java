/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.plugin.importing.variable;

import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class PluginVariableDto {

  /**
   * The id of the variable.
   *
   * <p>Note: This field is required in order to be imported to Optimize. Also the id must be
   * unique. Otherwise the variable might not be imported at all.
   */
  private String id;

  /**
   * The name of the variable.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private String name;

  /**
   * The type of the variable. This can be all primitive types that are supported by the engine. In
   * particular, String, Integer, Long, Short, Double, Boolean, Date.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private String type;

  /** The value of the variable. */
  private String value;

  /**
   * The timestamp of the last update to the variable.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private OffsetDateTime timestamp;

  /**
   * A map containing additional, value-type-dependent properties.
   *
   * <p>For variables of type Object, the following properties are returned:
   *
   * <p>objectTypeName: A string representation of the object's type name, e.g.
   * "com.example.MyObject". serializationDataFormat: The serialization format used to store the
   * variable, e.g. "application/xml".
   */
  private Map<String, Object> valueInfo;

  /**
   * The process definition key of the process model, where the variable was created.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private String processDefinitionKey;

  /**
   * The process definition id of the process model, where the variable was used.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private String processDefinitionId;

  /**
   * The process instance id of the process instance, where the variable was used.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private String processInstanceId;

  /**
   * The version of the variable value. While a process instance is running the same variable can be
   * updated several times. This value indicates which update number this variable is.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private Long version;

  /**
   * The field states the engine the variable is coming from. In Optimize you can configure multiple
   * engines to import data from. Each engine configuration should have an unique engine alias
   * associated with it.
   *
   * <p>Note: This field is required in order to be imported to Optimize.
   */
  private String engineAlias;

  /**
   * The field states the tenant this variable instance belongs to.
   *
   * <p>Note: Might be null if no tenant is assigned.
   */
  private String tenantId;
}
