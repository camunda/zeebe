/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.pub;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.variable.DefinitionVariableLabelsDto;
import io.camunda.optimize.rest.AbstractVariableLabelIT;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class PublicApiProcessVariableLabelIT extends AbstractVariableLabelIT {

  private final String ACCESS_TOKEN = "aToken";

  @BeforeEach
  public void setup() {
    embeddedOptimizeExtension
        .getConfigurationService()
        .getOptimizeApiConfiguration()
        .setAccessToken(ACCESS_TOKEN);
  }

  @Test
  public void updateVariableLabelsWithoutAccessToken() {
    // given
    final DefinitionVariableLabelsDto definitionVariableLabelsDto =
        new DefinitionVariableLabelsDto(PROCESS_DEFINITION_KEY, Collections.emptyList());

    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildProcessVariableLabelRequest(definitionVariableLabelsDto, null)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Override
  protected Response executeUpdateProcessVariableLabelRequest(
      final DefinitionVariableLabelsDto labelOptimizeDto) {
    return embeddedOptimizeExtension
        .getRequestExecutor()
        .buildProcessVariableLabelRequest(labelOptimizeDto, ACCESS_TOKEN)
        .execute();
  }
}
