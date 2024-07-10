/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;

public final class DateUtils {

  private DateUtils() {}

  // 2021-09-19T10:45:41
  //    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
  //            .appendPattern("yyyy-MM-dd[ HH:mm:ss]")
  //            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
  //            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
  //            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
  //            .toFormatter();

  public static final DateTimeFormatter DATE_FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("dd/MM/yyyy").toFormatter();

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .append(ISO_LOCAL_DATE)
          .optionalStart()
          .appendLiteral('T')
          .append(ISO_LOCAL_TIME)
          .optionalStart()
          .appendLiteral('Z')
          .optionalEnd()
          .optionalEnd()
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
          .toFormatter()
          .withChronology(IsoChronology.INSTANCE)
          .withResolverStyle(ResolverStyle.STRICT);

  public static LocalDate parseToLocalDate(String str) {
    return LocalDate.parse(str, DATE_TIME_FORMATTER);
  }

  public static LocalDateTime parseToLocalDateTime(String str) {
    return LocalDateTime.parse(str, DATE_TIME_FORMATTER);
  }

  public static boolean isLocalDate(String str) {
    try {
      LocalDate.parse(str);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  public static boolean isLocalDateTime(String str) {
    try {
      LocalDateTime.parse(str);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
