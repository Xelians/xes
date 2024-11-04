/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.core.JsonGenerator;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public class RulesReporter {

  public static void write(
      OperationDb operation, Path path, List<String> insertedRules, List<String> deletedRules)
      throws IOException {

    OutputStream os = Files.newOutputStream(path);
    try (JsonGenerator generator = JsonService.createGenerator(os)) {
      generator.writeStartObject();
      generator.writeStringField("Type", ReportType.RULES_REFERENTIAL.toString());
      generator.writeStringField("Date", LocalDateTime.now().toString());
      generator.writeNumberField("Tenant", operation.getTenant());
      generator.writeStringField("Status", ReportStatus.OK.toString());

      generator.writeFieldName("operation");
      generator.writeStartObject();
      generator.writeStringField("evId", operation.getId().toString());
      generator.writeStringField("evDateTime", operation.getCreated().toString());
      generator.writeStringField("evType", operation.getType().toString());
      generator.writeStringField(
          "outMessg", "Succès du processus d'import du référentiel des règles de gestion");
      generator.writeEndObject();
      JsonUtils.writeStringsField(generator, "insertedRules", insertedRules);
      JsonUtils.writeStringsField(generator, "deletedRules", deletedRules);
      generator.writeEndObject();
    }
  }
}
