/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitParser;
import fr.xelians.esafe.archive.domain.search.update.JsonPatchBuilder;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.search.domain.dsl.parser.NamedField;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.EqlParser;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * The Unset operator
 *
 * <pre>
 * {@code <script>}
 * { "$unset": [ "StartDate", "EndDate" ] }
 * {@code </script>}
 * </pre>
 */
public class Unset extends Patch {

  private static final String MANAGEMENT = "#management";

  private static final String[] excludedFieldNames = {
    "Title", "PhysicalDataObjects", "BinaryDataObjects", "SystemId"
  };

  private final List<String> fieldNames = new ArrayList<>();

  public Unset(EqlParser parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext);

    setParameter(node);
  }

  @Override
  public String name() {
    return "$unset";
  }

  protected void setParameter(JsonNode node) {
    // Extract JSON Parameters
    List<String> keys = new ArrayList<>();

    if (!node.isArray()) {
      throw new BadRequestException(
          String.format(CREATION_FAILED, this.name()), "Update fields must be an array");
    }

    for (JsonNode anode : node) {
      if (!anode.isTextual()) {
        throw new BadRequestException(
            String.format(CREATION_FAILED, this.name()),
            String.format("Update fields '%s' must be a string", anode.asText()));
      }
      keys.add(anode.asText());
    }

    if (keys.isEmpty()) {
      throw new BadRequestException(
          String.format(CREATION_FAILED, this.name()), "Update fields must not be empty");
    }

    AccessContractDb ac = ((ArchiveUnitParser) parser).getAccessContract();
    boolean isRestricted = Utils.isFalse(ac.getWritingRestrictedDesc());

    // Process base fields
    for (String key : keys) {

      // Check access contract writing restriction
      if (isRestricted && (key.startsWith(MANAGEMENT) || DOCUMENT_TYPE.equals(key))) {
        // TODO si on change les règles de gestion il faut penser à recalculer les dates
        // d'élimination
        throw new BadRequestException(
            CREATION_FAILED,
            String.format(
                "Access Contrat '%s' restricts modification of field '%s'",
                ac.getIdentifier(), key));
      }

      // Check and get field name path
      NamedField nameField = parser.getUpdateNameField(docType, key);

      if (StringUtils.startsWithAny(nameField.fieldName(), excludedFieldNames)) {
        throw new BadRequestException(CREATION_FAILED, String.format("'%s' cannot be unset", key));
      }

      fieldNames.add(toPath(nameField.fieldName()));
    }
  }

  private String toPath(String name) {
    return "/" + name.replace('.', '/');
  }

  @Override
  public JsonNode getJsonPatchOp() {
    JsonPatchBuilder jpb = new JsonPatchBuilder();
    fieldNames.forEach(jpb::remove);
    return jpb.build();
  }
}
