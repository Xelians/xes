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

public class ObjectConverter implements Converter {

  public static final ObjectConverter INSTANCE = new ObjectConverter();

  private ObjectConverter() {
    // Default init
  }

  public JsonNode convert(JsonNode srcNode) {
    ObjectNode dstNode = JsonNodeFactory.instance.objectNode();
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
      case "_tenant":
        dstNode.set("#tenant", entry.getValue());
        break;
      case "_qualifiers":
        dstNode.set("#qualifiers", convertQualifiers(entry.getValue()));
        break;
      case "_up":
        convertUp(dstNode, entry.getValue());
        break;
      case "_us":
        convertUs(dstNode, entry.getValue());
        break;
      case "_sp":
        dstNode.set("#originating_agency", entry.getValue());
        break;
      case "_sps":
        dstNode.set("#originating_agencies", entry.getValue());
        break;
      case "_opi":
        dstNode.put("#opi", entry.getValue().asText());
        break;
      case "_ops":
        convertOperations(dstNode, entry.getValue());
        break;
      case "_av":
        dstNode.set("#version", entry.getValue());
        break;
      case "_nbObjects":
        dstNode.set("#nbobjects", entry.getValue());
        break;
      case "_creationDate":
        dstNode.set("#approximate_creation_date", entry.getValue());
        break;
      case "_updateDate":
        dstNode.set("#approximate_update_date", entry.getValue());
        break;
      case "_storage":
        dstNode.set("#storage", entry.getValue());
        break;
      default:
        break; // Remove fields
    }
  }

  static void convertOperations(ObjectNode dstNode, JsonNode value) {
    ArrayNode anode = dstNode.putArray("#operations");
    for (JsonNode node : value) {
      anode.add(node.asText());
    }
  }

  static void convertUp(ObjectNode dstNode, JsonNode value) {
    ArrayNode anode = dstNode.putArray("#unitups");
    String v = value.asText();
    if (!v.equals("-1")) anode.add(v);
  }

  static void convertUs(ObjectNode dstNode, JsonNode value) {
    ArrayNode anode = dstNode.putArray("#allunitups");
    for (JsonNode node : value) {
      String v = node.asText();
      if (!v.equals("-1")) anode.add(v);
    }
  }

  static ArrayNode convertQualifiers(JsonNode srcQualifiersNodes) {
    ArrayNode dstNode = JsonNodeFactory.instance.arrayNode();

    for (JsonNode srcQualifiersNode : srcQualifiersNodes) {
      ObjectNode dstQualifiersNode = JsonNodeFactory.instance.objectNode();
      for (var ite = srcQualifiersNode.fields(); ite.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = ite.next();
        String srcKey = entry.getKey();
        switch (srcKey) {
          case "_nbc":
            dstQualifiersNode.set("#nbc", entry.getValue());
            break;
          case "versions":
            dstQualifiersNode.set("versions", convertVersions(entry.getValue()));
            break;
          default:
            dstQualifiersNode.set(srcKey, entry.getValue());
        }
      }
      dstNode.add(dstQualifiersNode);
    }
    return dstNode;
  }

  private static ArrayNode convertVersions(JsonNode srcVersionNodes) {
    ArrayNode dstNode = JsonNodeFactory.instance.arrayNode();

    for (JsonNode srcVersionNode : srcVersionNodes) {
      ObjectNode dstVersionNode = JsonNodeFactory.instance.objectNode();
      for (var ite = srcVersionNode.fields(); ite.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = ite.next();
        String srcKey = entry.getKey();
        switch (srcKey) {
          case "_id":
            dstVersionNode.put("#id", entry.getValue().asText());
            break;
          case "_opi":
            dstVersionNode.put("#opi", entry.getValue().asText());
            break;
          case "_pos":
            // Remove field
            break;
          default:
            dstVersionNode.set(srcKey, entry.getValue());
        }
      }
      dstNode.add(dstVersionNode);
    }
    return dstNode;
  }
}
