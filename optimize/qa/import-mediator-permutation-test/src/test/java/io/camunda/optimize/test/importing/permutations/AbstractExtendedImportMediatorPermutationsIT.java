/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.test.importing.permutations;
//
// import com.google.common.collect.Collections2;
// import io.camunda.optimize.service.importing.ImportMediator;
// import io.camunda.optimize.service.importing.permutations.AbstractImportMediatorPermutationsIT;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.List;
// import java.util.stream.Stream;
// import org.slf4j.Logger;
//
// public abstract class AbstractExtendedImportMediatorPermutationsIT
//     extends AbstractImportMediatorPermutationsIT {
//   private static final int MAX_MEDIATORS_TO_PERMUTATE = 8;
//
//   protected void logMediatorOrder(
//       final Logger logger, List<Class<? extends ImportMediator>> mediatorOrder) {
//     StringBuilder order = new StringBuilder();
//     mediatorOrder.forEach(mediator -> order.append(mediator.getSimpleName()).append("\n"));
//
//     logger.warn("Testing the following mediators order: \n" + order);
//   }
//
//   protected static Stream<List<Class<? extends ImportMediator>>> getMediatorPermutationsStream(
//       final List<Class<? extends ImportMediator>> requiredMediatorClasses,
//       List<Class<? extends ImportMediator>> additionalMediatorClasses) {
//
//     Collections.shuffle(additionalMediatorClasses);
//     additionalMediatorClasses =
//         additionalMediatorClasses.subList(
//             0, MAX_MEDIATORS_TO_PERMUTATE - requiredMediatorClasses.size());
//
//     List<Class<? extends ImportMediator>> importMediatorsToUse =
//         new ArrayList<>(requiredMediatorClasses);
//     importMediatorsToUse.addAll(additionalMediatorClasses);
//
//     return Collections2.permutations(importMediatorsToUse).stream();
//   }
// }
