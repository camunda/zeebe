/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.rest;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.dto.optimize.query.variable.DefinitionVariableLabelsDto;
// import jakarta.ws.rs.core.Response;
// import java.util.Collections;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class ProcessVariableLabelRestServiceIT extends AbstractVariableLabelIT {
//
//   @Test
//   public void updateVariableLabelForUnauthenticatedUser() {
//     // given
//     final DefinitionVariableLabelsDto definitionVariableLabelsDto =
//         new DefinitionVariableLabelsDto(PROCESS_DEFINITION_KEY, Collections.emptyList());
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildProcessVariableLabelRequest(definitionVariableLabelsDto)
//             .withoutAuthentication()
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
//   }
//
//   @Override
//   protected Response executeUpdateProcessVariableLabelRequest(
//       final DefinitionVariableLabelsDto labelOptimizeDto) {
//     return embeddedOptimizeExtension
//         .getRequestExecutor()
//         .buildProcessVariableLabelRequest(labelOptimizeDto)
//         .execute();
//   }
// }
