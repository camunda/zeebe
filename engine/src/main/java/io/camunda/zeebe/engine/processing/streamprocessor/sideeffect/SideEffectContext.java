/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.streamprocessor.sideeffect;

import io.camunda.zeebe.engine.processing.message.command.SubscriptionCommandSender;

public interface SideEffectContext {

  SubscriptionCommandSender getSiSubscriptionCommandSender();
}
