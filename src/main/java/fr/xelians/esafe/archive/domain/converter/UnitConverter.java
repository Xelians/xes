/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public class UnitConverter implements Converter {

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
    ObjectNode dstNode = JsonNodeFactory.instance.objectNode();
    for (var ite = srcNode.fields(); ite.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = ite.next();
      convertRoot(dstNode, entry);
    }

    // TODO maybe we should add validComputedInheritedRules to index
    // dstNode.put("#validComputedInheritedRules", true);
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
        dstNode.set("#computedInheritedRules", entry.getValue());
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

  private static void convertLifeCycles(ObjectNode dstNode, JsonNode srcLfcNodes) {
    ArrayNode dstArrayNode = dstNode.putArray("#lifecycles");

    for (JsonNode srcLfcNode : srcLfcNodes) {
      ObjectNode dstLfcNode = JsonNodeFactory.instance.objectNode();
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
}
