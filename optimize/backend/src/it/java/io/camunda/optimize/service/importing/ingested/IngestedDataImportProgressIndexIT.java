/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.importing.ingested;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.service.db.DatabaseConstants.ENGINE_ALIAS_OPTIMIZE;
// import static
// io.camunda.optimize.service.db.DatabaseConstants.EXTERNAL_PROCESS_VARIABLE_INDEX_NAME;
// import static
// io.camunda.optimize.service.importing.ExternalVariableUpdateImportIndexHandler.EXTERNAL_VARIABLE_UPDATE_IMPORT_INDEX_DOC_ID;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.dto.optimize.index.TimestampBasedImportIndexDto;
// import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableDto;
// import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
// import io.camunda.optimize.service.importing.ExternalVariableUpdateImportIndexHandler;
// import java.util.List;
// import lombok.SneakyThrows;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class IngestedDataImportProgressIndexIT extends AbstractIngestedDataImportIT {
//
//   @Test
//   public void ingestedVariableDataImportProgressIsPersisted() {
//     // given
//     final ExternalProcessVariableRequestDto externalVariable =
//         ingestionClient.createPrimitiveExternalVariable();
//     ingestionClient.ingestVariables(List.of(externalVariable));
//
//     // when
//     importIngestedDataFromScratchRefreshIndicesBeforeAndAfter();
//     embeddedOptimizeExtension.storeImportIndexesToElasticsearch();
//
//     // then
//     final long lastImportedExternalVariableTimestamp =
//         databaseIntegrationTestExtension
//             .getLastImportTimestampOfTimestampBasedImportIndex(
//                 EXTERNAL_VARIABLE_UPDATE_IMPORT_INDEX_DOC_ID, ENGINE_ALIAS_OPTIMIZE)
//             .toInstant()
//             .toEpochMilli();
//
//     assertThat(lastImportedExternalVariableTimestamp)
//         .isEqualTo(getAllStoredExternalProcessVariables().get(0).getIngestionTimestamp());
//   }
//
//   @Test
//   @SneakyThrows
//   public void indexProgressIsRestoredAfterRestartOfOptimize() {
//     // given
//     startAndUseNewOptimizeInstance();
//     final ExternalProcessVariableRequestDto externalVariable =
//         ingestionClient.createPrimitiveExternalVariable();
//     ingestionClient.ingestVariables(List.of(externalVariable));
//
//     // when
//     importIngestedDataFromScratchRefreshIndicesBeforeAndAfter();
//     embeddedOptimizeExtension.storeImportIndexesToElasticsearch();
//
//     final long lastImportedExternalVariableTimestamp =
//         databaseIntegrationTestExtension
//             .getLastImportTimestampOfTimestampBasedImportIndex(
//                 EXTERNAL_VARIABLE_UPDATE_IMPORT_INDEX_DOC_ID, ENGINE_ALIAS_OPTIMIZE)
//             .toInstant()
//             .toEpochMilli();
//
//     startAndUseNewOptimizeInstance();
//
//     // then
//     assertThat(
//             embeddedOptimizeExtension
//                 .getIndexHandlerRegistry()
//                 .getExternalVariableUpdateImportIndexHandler())
//         .extracting(ExternalVariableUpdateImportIndexHandler::getIndexStateDto)
//         .extracting(TimestampBasedImportIndexDto::getTimestampOfLastEntity)
//         .satisfies(
//             timestampOfLastEntity ->
//                 assertThat(timestampOfLastEntity.toInstant().toEpochMilli())
//                     .isEqualTo(lastImportedExternalVariableTimestamp));
//   }
//
//   private List<ExternalProcessVariableDto> getAllStoredExternalProcessVariables() {
//     return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
//         EXTERNAL_PROCESS_VARIABLE_INDEX_NAME, ExternalProcessVariableDto.class);
//   }
// }
