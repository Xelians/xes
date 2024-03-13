/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.logbook.domain.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public class LogbookConverter {

  private LogbookConverter() {
    // Default init
  }

  public static JsonNode convert(JsonNode srcNode) {
    return doConvert(srcNode);
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
      case "_operationId":
        dstNode.set("#id", entry.getValue());
        break;
      case "_tenant":
        dstNode.set("#tenant", entry.getValue());
        break;
      case "Type":
        dstNode.set("evTypeProc", entry.getValue());
        break;
      case "TypeInfo":
        dstNode.set("evType", entry.getValue());
        break;
      case "Message":
        dstNode.set("outMessg", entry.getValue());
        break;
      case "ObjectIdentifier":
        dstNode.set("obId", entry.getValue());
        break;
      case "ObjectInfo":
        dstNode.set("obIdReq", entry.getValue());
        break;
      case "Modified":
        dstNode.set("_lastPersistedDate", entry.getValue());
        break;
      case "ApplicationId":
        dstNode.set("evIdAppSession", entry.getValue());
        break;
      case "ObjectData":
        dstNode.set("evDetData", entry.getValue());
        break;
      case "Outcome":
        dstNode.set("outcome", entry.getValue());
        break;
      case "Created":
        dstNode.set("evDateTime", entry.getValue());
        break;
      case "UserIdentifier":
        dstNode.set("agId", entry.getValue());
        break;
      case "_secureNumber":
        break; // Remove fields
      default:
        dstNode.set(srcKey, entry.getValue());
    }
  }
}
