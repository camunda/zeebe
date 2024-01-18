/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.bpmn.task;

import io.camunda.zeebe.engine.processing.bpmn.BpmnElementContext;
import io.camunda.zeebe.engine.processing.bpmn.BpmnElementProcessor;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnBehaviors;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnCompensationSubscriptionBehaviour;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnEventSubscriptionBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnIncidentBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnJobBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnStateBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnStateTransitionBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnVariableMappingBehavior;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableJobWorkerTask;
import io.camunda.zeebe.util.Either;

/**
 * A BPMN processor for tasks that are based on jobs and should be processed by job workers. For
 * example, service tasks.
 */
public final class JobWorkerTaskProcessor implements BpmnElementProcessor<ExecutableJobWorkerTask> {

  private final BpmnIncidentBehavior incidentBehavior;
  private final BpmnStateTransitionBehavior stateTransitionBehavior;
  private final BpmnVariableMappingBehavior variableMappingBehavior;
  private final BpmnEventSubscriptionBehavior eventSubscriptionBehavior;
  private final BpmnJobBehavior jobBehavior;
  private final BpmnStateBehavior stateBehavior;
  private final BpmnCompensationSubscriptionBehaviour compensationSubscriptionBehaviour;

  public JobWorkerTaskProcessor(
      final BpmnBehaviors behaviors, final BpmnStateTransitionBehavior stateTransitionBehavior) {
    eventSubscriptionBehavior = behaviors.eventSubscriptionBehavior();
    incidentBehavior = behaviors.incidentBehavior();
    this.stateTransitionBehavior = stateTransitionBehavior;
    variableMappingBehavior = behaviors.variableMappingBehavior();
    jobBehavior = behaviors.jobBehavior();
    stateBehavior = behaviors.stateBehavior();
    compensationSubscriptionBehaviour = behaviors.compensationSubscriptionBehaviour();
  }

  @Override
  public Class<ExecutableJobWorkerTask> getType() {
    return ExecutableJobWorkerTask.class;
  }

  @Override
  public void onActivate(final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    variableMappingBehavior
        .applyInputMappings(context, element)
        .ifLeft(failure -> incidentBehavior.createIncident(failure, context));
  }

  @Override
  public void finalizeActivation(
      final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    jobBehavior
        .evaluateJobExpressions(element, context)
        .flatMap(j -> eventSubscriptionBehavior.subscribeToEvents(element, context).map(ok -> j))
        .ifRightOrLeft(
            jobProperties -> {
              jobBehavior.createNewJob(context, element, jobProperties);
              stateTransitionBehavior.transitionToActivated(context, element.getEventType());
            },
            failure -> incidentBehavior.createIncident(failure, context));
  }

  @Override
  public void onComplete(final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    // nothing to do
  }

  @Override
  public void finalizeCompletion(
      final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    variableMappingBehavior
        .applyOutputMappings(context, element)
        .flatMap(
            ok -> {
              eventSubscriptionBehavior.unsubscribeFromEvents(context);
              compensationSubscriptionBehaviour.createCompensationSubscription(element, context);
              return stateTransitionBehavior.transitionToCompleted(element, context);
            })
        .flatMap(
            completionContext -> {
              compensationSubscriptionBehaviour.completeCompensationHandler(context, element);
              stateTransitionBehavior.takeOutgoingSequenceFlows(element, completionContext);
              return Either.right(context);
            });
  }

  @Override
  public void onTerminate(final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    final var flowScopeInstance = stateBehavior.getFlowScopeInstance(context);

    jobBehavior.cancelJob(context);
    eventSubscriptionBehavior.unsubscribeFromEvents(context);
    incidentBehavior.resolveIncidents(context);

    eventSubscriptionBehavior
        .findEventTrigger(context)
        .filter(eventTrigger -> flowScopeInstance.isActive())
        .filter(eventTrigger -> !flowScopeInstance.isInterrupted())
        .ifPresentOrElse(
            eventTrigger -> {
              final var terminated =
                  stateTransitionBehavior.transitionToTerminated(context, element.getEventType());
              eventSubscriptionBehavior.activateTriggeredEvent(
                  context.getElementInstanceKey(),
                  terminated.getFlowScopeKey(),
                  eventTrigger,
                  terminated);
            },
            () -> {
              final var terminated =
                  stateTransitionBehavior.transitionToTerminated(context, element.getEventType());
              stateTransitionBehavior.onElementTerminated(element, terminated);
            });
  }
}
