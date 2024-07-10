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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;

/**
 * The Set operator.
 *
 * <pre>
 * {@code <script>}
 * "$set": { "Description" : "The Description" }
 * {@code </script>}
 * </pre>
 *
 * <pre>
 * {@code <script>}
 * "$set": { "#management.AccessRule.Rules": [
 *     {
 *       "Rule": "ACC-00001",
 *       "StartDate": "2018-12-04"
 *     }
 *   ]
 * }
 * {@code </script>}
 * </pre>
 */
public class Set extends Patch {

  private static final String MANAGEMENT = "management";

  private static final String[] excludedFieldNames = {
    "BinaryDataObjects", "SystemId",
  };

  private final List<FieldNode> pathFieldNodes = new ArrayList<>();

  public Set(EqlParser parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext);
    setParameter(node);
  }

  @Override
  public String name() {
    return "$set";
  }

  protected void setParameter(JsonNode node) {

    List<FieldNode> baseFieldNodes = new ArrayList<>();
    List<FieldNode> fullFieldNodes = new ArrayList<>();

    for (Iterator<Entry<String, JsonNode>> i = node.fields(); i.hasNext(); ) {
      Entry<String, JsonNode> entry = i.next();
      baseFieldNodes.add(new FieldNode(entry.getKey(), entry.getValue()));
      extractParameter(entry.getKey(), entry.getValue(), fullFieldNodes);
    }

    if (baseFieldNodes.isEmpty()) {
      throw new BadRequestException(
          String.format(CREATION_FAILED, this.name()), "Update field is empty or does not exist");
    }

    AccessContractDb ac = ((ArchiveUnitParser) parser).getAccessContract();
    boolean isRestricted = Utils.isFalse(ac.getWritingRestrictedDesc());

    // Process base fields
    for (FieldNode bfn : baseFieldNodes) {

      // Check access contract writing restriction
      if (isRestricted
          && (bfn.fieldName().startsWith(MANAGEMENT) || DOCUMENT_TYPE.equals(bfn.fieldName()))) {
        // TODO si on change les règles de gestion il faut penser à recalculer les dates
        // d'élimination
        throw new BadRequestException(
            CREATION_FAILED,
            String.format(
                "Access Contrat '%s' restricts modification of field '%s'",
                ac.getIdentifier(), bfn.fieldName()));
      }

      // Check and get field name path
      NamedField nameField = parser.getUpdateNameField(docType, bfn.fieldName);
      pathFieldNodes.add(new FieldNode(toPath(nameField.fieldName()), bfn.valueNode));
    }

    // Process full fields
    for (FieldNode ffn : fullFieldNodes) {

      // Check and get field
      NamedField nameField = parser.getUpdateNameField(docType, ffn.fieldName);

      if (nameField.field() == null) {
        throw new BadRequestException(
            CREATION_FAILED, String.format("Field '%s' does not exist in mapping", ffn.fieldName));
      }

      if (StringUtils.startsWithAny(nameField.fieldName(), excludedFieldNames)) {
        throw new BadRequestException(
            CREATION_FAILED, String.format("Value of field '%s' cannot be updated", ffn.fieldName));
      }

      try {
        nameField.field().asValue(ffn.valueNode);
      } catch (BadRequestException ex) {
        throw new BadRequestException(
            String.format(CREATION_FAILED, this.name()),
            String.format(
                "Field '%s' with type '%s' and value '%s' of type '%s' mismatch",
                ffn.fieldName,
                nameField.field().getType(),
                ffn.valueNode.asText(),
                ffn.valueNode.getClass().getSimpleName().toLowerCase().replace("node", "")));
      }
    }
  }

  private String toPath(String name) {
    return "/" + name.replace('.', '/');
  }

  private void extractParameter(String fieldName, JsonNode valueNode, List<FieldNode> fieldNodes) {
    if (valueNode.isArray()) {
      for (JsonNode node : valueNode) {
        extractParameter(fieldName, node, fieldNodes);
      }
    } else if (valueNode.isObject()) {
      for (Iterator<Entry<String, JsonNode>> i = valueNode.fields(); i.hasNext(); ) {
        Entry<String, JsonNode> entry = i.next();
        extractParameter(fieldName + "." + entry.getKey(), entry.getValue(), fieldNodes);
      }
    } else {
      fieldNodes.add(new FieldNode(fieldName, valueNode));
    }
  }

  @Override
  public JsonNode getJsonPatchOp() {
    JsonPatchBuilder jpb = new JsonPatchBuilder();
    pathFieldNodes.forEach(pn -> jpb.add(pn.fieldName, pn.valueNode));
    return jpb.build();
  }

  private record FieldNode(String fieldName, JsonNode valueNode) {}
}
