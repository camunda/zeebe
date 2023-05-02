/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.bpmn;

import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnStateBehavior;
import io.camunda.zeebe.engine.processing.common.Failure;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableActivity;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableBoundaryEvent;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableFlowElement;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableFlowNode;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableProcess;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableSequenceFlow;
import io.camunda.zeebe.engine.state.deployment.DeployedProcess;
import io.camunda.zeebe.engine.state.instance.ElementInstance;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.util.Either;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;

/**
 * Checks the preconditions of a state transition command.
 *
 * <p>A process instance can be have concurrent state transitions if a user command is received
 * (e.g. cancel process instance) or if an internal/external event is triggered (e.g. timer boundary
 * event). In this case, the current process instance processing needs to be interrupted be avoid an
 * inconsistent state.
 */
public final class ProcessInstanceStateTransitionGuard {

  private final BpmnStateBehavior stateBehavior;

  public ProcessInstanceStateTransitionGuard(final BpmnStateBehavior stateBehavior) {
    this.stateBehavior = stateBehavior;
  }

  /**
   * Checks if the preconditions of the given command are met.
   *
   * @return {@code true} if the preconditions are met and the transition command is valid.
   */
  public Either<Failure, ?> isValidStateTransition(
      final BpmnElementContext context, final ExecutableFlowElement element) {
    return checkStateTransition(context, element).mapLeft(Failure::new);
  }

  private Either<String, ?> checkStateTransition(
      final BpmnElementContext context, final ExecutableFlowElement element) {

    return switch (context.getIntent()) {
      case ACTIVATE_ELEMENT ->
          hasActiveFlowScopeInstance(context)
              .flatMap(ok -> canActivateParallelGateway(context, element))
              .flatMap(ok -> canActivateInclusiveGateway(context, element));
      case COMPLETE_ELEMENT ->
          // an incident is resolved by writing a COMPLETE command when the element instance is in
          // state COMPLETING
          hasElementInstanceWithState(
                  context,
                  ProcessInstanceIntent.ELEMENT_ACTIVATED,
                  ProcessInstanceIntent.ELEMENT_COMPLETING)
              .flatMap(ok -> hasActiveFlowScopeInstance(context));
      case TERMINATE_ELEMENT ->
          hasElementInstanceWithState(
              context,
              ProcessInstanceIntent.ELEMENT_ACTIVATING,
              ProcessInstanceIntent.ELEMENT_ACTIVATED,
              ProcessInstanceIntent.ELEMENT_COMPLETING);
      default ->
          Either.left(
              String.format(
                  "Expected the check of the preconditions of a command with intent [activate,complete,terminate] but the intent was '%s'",
                  context.getIntent()));
    };
  }

  private Either<String, ElementInstance> getElementInstance(final BpmnElementContext context) {
    final var elementInstance = stateBehavior.getElementInstance(context);
    if (elementInstance != null) {
      return Either.right(elementInstance);

    } else {
      return Either.left(
          String.format(
              "Expected element instance with key '%d' to be present in state but not found.",
              context.getElementInstanceKey()));
    }
  }

  private Either<String, ElementInstance> getFlowScopeInstance(final BpmnElementContext context) {
    final var flowScopeInstance = stateBehavior.getFlowScopeInstance(context);
    if (flowScopeInstance != null) {
      return Either.right(flowScopeInstance);

    } else {
      return Either.left(
          String.format(
              "Expected flow scope instance with key '%d' to be present in state but not found.",
              context.getFlowScopeKey()));
    }
  }

  private Either<String, ElementInstance> hasElementInstanceInState(
      final ElementInstance elementInstance,
      final ProcessInstanceIntent expectedState,
      final ProcessInstanceIntent... otherExpected) {
    final var currentState = elementInstance.getState();
    if (expectedState != currentState && !Arrays.asList(otherExpected).contains(currentState)) {
      return Either.left(
          String.format(
              "Expected element instance to be in state '%s' or one of '%s' but was '%s'.",
              expectedState, Arrays.toString(otherExpected), currentState));
    } else {
      return Either.right(elementInstance);
    }
  }

  private Either<String, ?> hasElementInstanceWithState(
      final BpmnElementContext context,
      final ProcessInstanceIntent expectedState,
      final ProcessInstanceIntent... otherExpected) {
    // a shortcut to improve readability
    return getElementInstance(context)
        .flatMap(
            elementInstance ->
                hasElementInstanceInState(elementInstance, expectedState, otherExpected));
  }

  private Either<String, ElementInstance> hasFlowScopeInstanceInState(
      final ElementInstance flowScopeInstance, final ProcessInstanceIntent expectedState) {
    final var currentState = flowScopeInstance.getState();
    if (currentState != expectedState) {
      return Either.left(
          String.format(
              "Expected flow scope instance to be in state '%s' but was '%s'.",
              expectedState, currentState));

    } else {
      return Either.right(flowScopeInstance);
    }
  }

  private Either<String, ElementInstance> hasNonInterruptedFlowScope(
      final ElementInstance flowScopeInstance, final BpmnElementContext context) {
    final var interruptingElementId = flowScopeInstance.getInterruptingElementId();
    if (flowScopeInstance.isInterrupted()
        && !interruptingElementId.equals(context.getElementId())) {
      return Either.left(
          String.format(
              "Expected flow scope instance to be not interrupted but was interrupted by an event with id '%s'.",
              BufferUtil.bufferAsString(interruptingElementId)));

    } else {
      return Either.right(flowScopeInstance);
    }
  }

  private Either<String, ?> hasActiveFlowScopeInstance(final BpmnElementContext context) {
    // a shortcut to improve readability
    if (context.getBpmnElementType() == BpmnElementType.PROCESS) {
      // a process has no flow scope instance
      return Either.right(null);

    } else {
      return getFlowScopeInstance(context)
          .flatMap(
              flowScopeInstance ->
                  hasFlowScopeInstanceInState(
                      flowScopeInstance, ProcessInstanceIntent.ELEMENT_ACTIVATED))
          .flatMap(flowScopeInstance -> hasNonInterruptedFlowScope(flowScopeInstance, context));
    }
  }

  private Either<String, ?> canActivateParallelGateway(
      final BpmnElementContext context, final ExecutableFlowElement executableFlowElement) {
    if (context.getBpmnElementType() != BpmnElementType.PARALLEL_GATEWAY) {
      return Either.right(null);
    } else {
      final var element = (ExecutableFlowNode) executableFlowElement;
      final int numberOfIncomingSequenceFlows = element.getIncoming().size();
      final int numberOfTakenSequenceFlows =
          stateBehavior.getNumberOfTakenSequenceFlows(context.getFlowScopeKey(), element.getId());
      return numberOfTakenSequenceFlows >= numberOfIncomingSequenceFlows
          ? Either.right(null)
          : Either.left(
              String.format(
                  "Expected to be able to activate parallel gateway '%s',"
                      + " but not all sequence flows have been taken.",
                  BufferUtil.bufferAsString(element.getId())));
    }
  }

  private Either<String, ?> canActivateInclusiveGateway(
      final BpmnElementContext context, final ExecutableFlowElement executableFlowElement) {
    if (context.getBpmnElementType() == BpmnElementType.INCLUSIVE_GATEWAY) {
      final var element = (ExecutableFlowNode) executableFlowElement;
      final int numberOfIncomingSequenceFlows = element.getIncoming().size();
      final int numberOfTakenSequenceFlows =
          stateBehavior.getNumberOfTakenSequenceFlows(context.getFlowScopeKey(), element.getId());

      // Accept if all incoming sequence flows were taken at least once
      if (numberOfTakenSequenceFlows >= numberOfIncomingSequenceFlows) {
        return Either.right(null);
      }

      if (stateBehavior.getElementInstance(context.getElementInstanceKey()) == null) {
        return doesPathExist(context, element);
      }
    }

    // Accept if the element is not inclusive gateway or continue processing after incident
    // resolved.
    return Either.right(null);
  }

  private Either<String, ?> doesPathExist(
      final BpmnElementContext context, final ExecutableFlowElement executableFlowElement) {
    final Optional<DeployedProcess> deployedProcess =
        stateBehavior.getProcess(context.getProcessDefinitionKey(), context.getTenantId());

    return deployedProcess
        .map(DeployedProcess::getProcess)
        .map(process -> doesPathExist(context, executableFlowElement, process))
        .orElse(
            Either.left(
                String.format(
                    "Expected to find a deployed process for process definition key '%d', but none found.",
                    context.getProcessDefinitionKey())));
  }

  private Either<String, ?> doesPathExist(
      final BpmnElementContext context,
      final ExecutableFlowElement executableFlowElement,
      final ExecutableProcess process) {
    final var targetElementId = executableFlowElement.getId();
    final var activateElementIds = findActivateElementInFlowScope(context, targetElementId);
    final Optional<DirectBuffer> exists =
        activateElementIds.stream()
            .filter(
                elementId -> doesPathExist(process, elementId, targetElementId, new HashSet<>()))
            .findAny();

    if (exists.isPresent()) {
      return Either.left(
          String.format(
              "Expected to be able to activate inclusive gateway '%s',"
                  + " but not all satisfied sequence flows have been taken",
              BufferUtil.bufferAsString(targetElementId)));
    } else {
      return findPendingActivateElementInFlowScope(context, process, targetElementId);
    }
  }

  public Set<DirectBuffer> findActivateElementInFlowScope(
      final BpmnElementContext context, final DirectBuffer targetElementId) {
    final var flowScopeContext = stateBehavior.getFlowScopeContext(context);
    final var childInstances = stateBehavior.getChildInstances(flowScopeContext);

    // activate element id in flow scope
    return childInstances.stream()
        .map(BpmnElementContext::getElementId)
        .filter(elementId -> !elementId.equals(targetElementId))
        .collect(Collectors.toSet());
  }

  public Either<String, ?> findPendingActivateElementInFlowScope(
      final BpmnElementContext context,
      final ExecutableProcess process,
      final DirectBuffer targetElementId) {
    final var pendingSequenceFlowIds = stateBehavior.getPendingSequenceFlowIds(context);
    final var existsPathPendingSequenceFlowIds =
        pendingSequenceFlowIds.stream()
            .filter(
                flowId -> {
                  final DirectBuffer elementId =
                      process
                          .getElementById(flowId, ExecutableSequenceFlow.class)
                          .getTarget()
                          .getId();

                  return doesPathExist(process, elementId, targetElementId, new HashSet<>());
                })
            .collect(Collectors.toSet());

    final long count = existsPathPendingSequenceFlowIds.size();

    final Set<DirectBuffer> incomingSequenceFlows =
        process.getElementById(targetElementId, ExecutableFlowNode.class).getIncoming().stream()
            .map(ExecutableSequenceFlow::getId)
            .collect(Collectors.toSet());

    // Accept if the number of pending sequences to the gateway >= 1
    // and there are no other active paths to the gateway
    if (count >= 1 && incomingSequenceFlows.containsAll(existsPathPendingSequenceFlowIds)) {
      return Either.right(null);
    } else {
      return Either.left(
          String.format(
              "Expected to be able to activate inclusive gateway '%s',"
                  + " but not all satisfied sequence flows have been taken or"
                  + " the gateway was activated already.",
              BufferUtil.bufferAsString(targetElementId)));
    }
  }

  public boolean doesPathExist(
      final ExecutableProcess process,
      final DirectBuffer sourceElementId,
      final DirectBuffer targetElementId,
      final Set<DirectBuffer> visited) {
    // found path from sourceElement to targetElement
    if (sourceElementId.equals(targetElementId)) {
      return true;
    }

    if (!visited.contains(sourceElementId)) {
      visited.add(sourceElementId);
      final ExecutableFlowNode sourceElement =
          process.getElementById(sourceElementId, ExecutableFlowNode.class);
      final List<DirectBuffer> sourceElementIds =
          sourceElement.getOutgoing().stream()
              .map(ExecutableSequenceFlow::getTarget)
              .map(ExecutableFlowNode::getId)
              .collect(Collectors.toList());

      if (sourceElement instanceof ExecutableActivity) {
        final List<ExecutableBoundaryEvent> boundaryEvents =
            ((ExecutableActivity) sourceElement).getBoundaryEvents();
        boundaryEvents.forEach(event -> sourceElementIds.add(event.getId()));
      }

      return sourceElementIds.stream()
          .anyMatch(elementId -> doesPathExist(process, elementId, targetElementId, visited));
    }
    return false;
  }
}
