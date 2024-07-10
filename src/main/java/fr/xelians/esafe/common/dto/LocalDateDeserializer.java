/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public class LocalDateDeserializer extends StdDeserializer<LocalDate> {

  private static final DateTimeFormatter FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("d[d]/M[M]/yyyy").toFormatter();

  public LocalDateDeserializer() {
    this(null);
  }

  public LocalDateDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public LocalDate deserialize(JsonParser jsonparser, DeserializationContext context)
      throws IOException {

    String str = jsonparser.getText();
    try {
      return LocalDate.parse(str);
    } catch (DateTimeParseException e1) {
      try {
        return LocalDate.parse(str, FORMATTER);
      } catch (DateTimeParseException e2) {
        try {
          return LocalDateTime.parse(str).toLocalDate();
        } catch (DateTimeParseException e3) {
          throw new BadRequestException(String.format("Invalid date format: '%s'", str), e3);
        }
      }
    }
  }
}
