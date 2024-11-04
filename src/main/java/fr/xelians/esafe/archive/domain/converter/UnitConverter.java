/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

/*
 * @author Emmanuel Deviller
 */
public final class UnitConverter implements Converter {

  private static final String[] RULES_NAMES = {
    "AccessRule",
    "AppraisalRule",
    "DisseminationRule",
    "ReuseRule",
    "ClassificationRule",
    "StorageRule",
    "HoldRule"
  };

  public static final UnitConverter INSTANCE = new UnitConverter();

  private UnitConverter() {
    // Default init
  }

  public JsonNode convert(JsonNode srcNode) {
    return doConvert(srcNode);
  }

  public static JsonNode convertWithInheritedRules(JsonNode srcNode, JsonNode inheritedRules) {
    ObjectNode dstNode = doConvert(srcNode);
    dstNode.set("InheritedRules", inheritedRules);
    return dstNode;
  }

  private static ObjectNode doConvert(JsonNode srcNode) {
    ObjectNode dstNode = createObjectNode();
    for (var ite = srcNode.fields(); ite.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = ite.next();
      convertRoot(dstNode, entry);
    }
    return dstNode;
  }

  private static void convertRoot(ObjectNode dstNode, Map.Entry<String, JsonNode> entry) {
    String srcKey = entry.getKey();
    switch (srcKey) {
      case "_unitId":
        dstNode.put("#id", entry.getValue().asText());
        break;
      case "_objectId":
        dstNode.put("#object", entry.getValue().asText());
        break;
      case "_tenant":
        dstNode.set("#tenant", entry.getValue());
        break;
      case "_up":
        ObjectConverter.convertUp(dstNode, entry.getValue());
        break;
      case "_us":
        ObjectConverter.convertUs(dstNode, entry.getValue());
        break;
      case "_sp":
        dstNode.set("#originating_agency", entry.getValue());
        break;
      case "_sps":
        dstNode.set("#originating_agencies", entry.getValue());
        break;
      case "_qualifiers":
        dstNode.set("#qualifiers", ObjectConverter.convertQualifiers(entry.getValue()));
        break;
      case "_mgt":
        dstNode.set("#management", entry.getValue());
        break;
      case "_validCir":
        dstNode.set("#validComputedInheritedRules", entry.getValue());
        break;
      case "_cir":
        convertComputInheritedRules(dstNode, entry.getValue());
        break;
      case "_extents":
        convertExtents(dstNode, entry.getValue());
        break;
      case "_opi":
        dstNode.put("#opi", entry.getValue().asText());
        break;
      case "_ops":
        ObjectConverter.convertOperations(dstNode, entry.getValue());
        break;
      case "_av":
        dstNode.set("#version", entry.getValue());
        break;
      case "_min":
        dstNode.set("#min", entry.getValue());
        break;
      case "_max":
        dstNode.set("#max", entry.getValue());
        break;
      case "_nbObjects":
        dstNode.set("#nbobjects", entry.getValue());
        break;
      case "_unitType":
        dstNode.set("#unitType", entry.getValue());
        break;
      case "_creationDate":
        dstNode.set("#approximate_creation_date", entry.getValue());
        break;
      case "_updateDate":
        dstNode.set("#approximate_update_date", entry.getValue());
        break;
      case "_lifeCycles":
        convertLifeCycles(dstNode, entry.getValue());
        break;
      case "_storage":
        dstNode.set("#storage", entry.getValue());
        break;
      case "_sedaVersion":
        dstNode.set("#sedaversion", entry.getValue());
        break;
      case "_implementationVersion":
        dstNode.set("#implementationversion", entry.getValue());
        break;
      case "_transferred":
        dstNode.set("#transferred", entry.getValue());
        break;
      case "_ext", "_ups", "_keywords":
        break; // Remove fields
      default:
        dstNode.set(srcKey, entry.getValue());
    }
  }

  private static void convertExtents(ObjectNode dstNode, JsonNode extentsNode) {
    for (var ite = extentsNode.fields(); ite.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = ite.next();
      dstNode.set(entry.getKey(), entry.getValue());
    }
  }

  private static void convertComputInheritedRules(ObjectNode dstNode, JsonNode cirNode) {
    ObjectNode dstCirNode = createObjectNode();
    for (String rulesName : RULES_NAMES) {
      JsonNode rulesNode = cirNode.get(rulesName);
      dstCirNode.set(rulesName, rulesNode == null ? createObjectNode() : rulesNode);
    }
    dstNode.set("#computedInheritedRules", dstCirNode);
  }

  private static void convertLifeCycles(ObjectNode dstNode, JsonNode srcLfcNodes) {
    ArrayNode dstArrayNode = dstNode.putArray("#lifecycles");
    for (JsonNode srcLfcNode : srcLfcNodes) {
      ObjectNode dstLfcNode = createObjectNode();
      for (var ite = srcLfcNode.fields(); ite.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = ite.next();
        String srcKey = entry.getKey();
        String dstKey =
            switch (srcKey) {
              case "_opDate" -> "#lfc_date";
              case "_opi" -> "#lfc_opi";
              case "_patch" -> "#lfc_patch";
              case "_opType" -> "#lfc_type";
              case "_av" -> "#lfc_version";
              default -> srcKey;
            };
        dstLfcNode.set(dstKey, entry.getValue());
      }
      dstArrayNode.add(dstLfcNode);
    }
  }

  private static ObjectNode createObjectNode() {
    return JsonNodeFactory.instance.objectNode();
  }
}
