/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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

public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

  private static final DateTimeFormatter ALTERNATIVE_DATE_FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("d[d]/M[M]/yyyy").toFormatter();
  private static final DateTimeFormatter ALTERNATIVE_DATE_TIME_FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("d[d]/M[M]/yyyy HH:mm:ss").toFormatter();

  public CustomLocalDateTimeDeserializer() {
    this(null);
  }

  public CustomLocalDateTimeDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext context)
      throws IOException {

    String str = jsonparser.getText();
    try {
      return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException e1) {
      try {
        return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
      } catch (DateTimeParseException e2) {
        try {
          return LocalDateTime.parse(str, ALTERNATIVE_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e3) {
          try {
            return LocalDate.parse(str, ALTERNATIVE_DATE_FORMATTER).atStartOfDay();
          } catch (DateTimeParseException e4) {
            throw new BadRequestException(String.format("Invalid date format: '%s'", str), e4);
          }
        }
      }
    }
  }
}
