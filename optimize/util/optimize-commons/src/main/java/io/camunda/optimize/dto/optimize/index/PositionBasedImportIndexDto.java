/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.index;

import io.camunda.optimize.dto.optimize.datasource.ZeebeDataSourceDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants
public class PositionBasedImportIndexDto extends ImportIndexDto<ZeebeDataSourceDto> {

  protected long positionOfLastEntity = 0;
  protected long sequenceOfLastEntity = 0;
  protected String esTypeIndexRefersTo;
  protected boolean hasSeenSequenceField =
      false; // flag to indicate whether at least one record with a sequence field has been imported
}
