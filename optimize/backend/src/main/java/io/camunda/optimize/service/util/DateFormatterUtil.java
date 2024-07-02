/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util;

import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;

import com.github.sisyphsu.dateparser.DateParserUtils;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateFormatterUtil {

  private static final DateTimeFormatter OPTIMIZE_FORMATTER =
      DateTimeFormatter.ofPattern(OPTIMIZE_DATE_FORMAT);

  public static boolean isValidOptimizeDateFormat(final String value) {
    try {
      OffsetDateTime.parse(value, OPTIMIZE_FORMATTER);
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public static Optional<String> getDateStringInOptimizeDateFormat(final String dateString) {
    try {
      final OffsetDateTime parsedOffsetDateTime = DateParserUtils.parseOffsetDateTime(dateString);
      return Optional.of(parsedOffsetDateTime.format(OPTIMIZE_FORMATTER));
    } catch (DateTimeParseException ex) {
      return Optional.empty();
    }
  }
}
