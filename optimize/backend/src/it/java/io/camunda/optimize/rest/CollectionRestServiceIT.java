/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestUtil.getOffsetDiffInHours;
import static io.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_TIMEZONE;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.IdResponseDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.collection.PartialCollectionDataDto;
import io.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
import io.camunda.optimize.service.db.writer.CollectionWriter;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class CollectionRestServiceIT extends AbstractPlatformIT {

  @Test
  public void createNewCollectionWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withoutAuthentication()
            .buildCreateCollectionRequest()
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void createNewCollection() {
    // when
    String collectionId = collectionClient.createNewCollection();

    // then the status code is okay
    assertThat(collectionId).isNotNull();

    // and saved Collection has expected properties
    CollectionDefinitionRestDto savedCollectionDto =
        collectionClient.getCollectionById(collectionId);
    assertThat(savedCollectionDto.getName()).isEqualTo(CollectionWriter.DEFAULT_COLLECTION_NAME);
    assertThat(savedCollectionDto.getData().getConfiguration()).isEqualTo(Collections.EMPTY_MAP);
  }

  @Test
  public void createNewCollectionWithPartialDefinition() {
    // when
    String collectionName = "some collection";
    Map<String, String> configMap = Collections.singletonMap("Foo", "Bar");
    PartialCollectionDefinitionRequestDto partialCollectionDefinitionDto =
        new PartialCollectionDefinitionRequestDto();
    partialCollectionDefinitionDto.setName(collectionName);
    PartialCollectionDataDto partialCollectionDataDto = new PartialCollectionDataDto();
    partialCollectionDataDto.setConfiguration(configMap);
    partialCollectionDefinitionDto.setData(partialCollectionDataDto);
    IdResponseDto idDto =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildCreateCollectionRequestWithPartialDefinition(partialCollectionDefinitionDto)
            .execute(IdResponseDto.class, Response.Status.OK.getStatusCode());

    // then the status code is okay
    assertThat(idDto).isNotNull();

    // and saved Collection has expected properties
    CollectionDefinitionRestDto savedCollectionDto =
        collectionClient.getCollectionById(idDto.getId());
    assertThat(savedCollectionDto.getName()).isEqualTo(collectionName);
    assertThat(savedCollectionDto.getData().getConfiguration()).isEqualTo(configMap);
  }

  @Test
  public void updateCollectionWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withoutAuthentication()
            .buildUpdatePartialCollectionRequest("1", null)
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void updateNonExistingCollection() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildUpdatePartialCollectionRequest(
                "NonExistingId", new PartialCollectionDefinitionRequestDto())
            .execute();

    // given
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void updateNameOfCollection() {
    // given
    String id = collectionClient.createNewCollection();
    final PartialCollectionDefinitionRequestDto collectionRenameDto =
        new PartialCollectionDefinitionRequestDto("Test");

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildUpdatePartialCollectionRequest(id, collectionRenameDto)
            .execute();

    // then the status code is okay
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  public void getCollectionWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withoutAuthentication()
            .buildGetCollectionRequest("asdf")
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void getCollection() {
    // given
    String id = collectionClient.createNewCollection();

    // when
    CollectionDefinitionRestDto collection = collectionClient.getCollectionById(id);
    List<EntityResponseDto> collectionEntities = collectionClient.getEntitiesForCollection(id);

    // then
    assertThat(collection).isNotNull();
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collectionEntities).isEmpty();
    assertThat(collection.getOwner()).isEqualTo(DEFAULT_FULLNAME);
    assertThat(collection.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
  }

  @Test
  public void getCollection_adoptTimezoneFromHeader() {
    // given
    OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();
    String collectionId = collectionClient.createNewCollection();

    // when
    CollectionDefinitionRestDto collection =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetCollectionRequest(collectionId)
            .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
            .execute(CollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());

    // then
    assertThat(collection.getCreated()).isEqualTo(now);
    assertThat(collection.getLastModified()).isEqualTo(now);
    assertThat(getOffsetDiffInHours(collection.getCreated(), now)).isEqualTo(1.);
    assertThat(getOffsetDiffInHours(collection.getLastModified(), now)).isEqualTo(1.);
  }

  @Test
  public void getCollectionForNonExistingIdThrowsError() {
    // when
    String response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetCollectionRequest("fooid")
            .execute(String.class, Response.Status.NOT_FOUND.getStatusCode());

    // then the status code is okay
    assertThat(response).containsSequence("Collection does not exist!");
  }

  @Test
  public void deleteCollectionWithoutAuthentication() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withoutAuthentication()
            .buildDeleteCollectionRequest("1124")
            .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void deleteNewCollection() {
    // given
    String id = collectionClient.createNewCollection();

    // when
    Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildDeleteCollectionRequest(id).execute();

    // then the status code is okay
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

    final Response getByIdResponse =
        embeddedOptimizeExtension.getRequestExecutor().buildGetCollectionRequest(id).execute();
    assertThat(getByIdResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void deleteNonExitingCollection() {
    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteCollectionRequest("NonExistingId")
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }
}
