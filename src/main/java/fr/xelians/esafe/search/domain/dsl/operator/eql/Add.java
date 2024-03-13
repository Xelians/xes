/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.EqlParser;
import fr.xelians.esafe.search.domain.field.Field;
import java.util.Iterator;
import java.util.Map.Entry;

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
public class Add extends Reclassification {

  private static final String UNIT_UPS = "#unitups";
  private static final String UNIT_UP = "#unitup";

  protected Long value;

  public Add(EqlParser parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext);
    setParameter(node);
  }

  @Override
  public String name() {
    return "$add";
  }

  protected void setParameter(JsonNode node) {

    String fieldName = null;
    JsonNode valueNode = null;

    // Extract JSON Parameters
    for (Iterator<Entry<String, JsonNode>> i = node.fields(); i.hasNext(); ) {
      Entry<String, JsonNode> entry = i.next();
      String key = entry.getKey();
      if (UNIT_UPS.equals(key)) {
        if (fieldName != null) {
          throwBadRequestException("#unitup[s] is already defined");
        }
        fieldName = key;
        JsonNode valuesNode = entry.getValue();
        if (!valuesNode.isArray() || valuesNode.size() != 1) {
          throwBadRequestException(
              String.format("Field '%s' must be an array with one unique value", fieldName));
        }
        valueNode = valuesNode.get(0);
      } else if (UNIT_UP.equals(key)) {
        if (fieldName != null) {
          throwBadRequestException("#unitup[s] is already defined");
        }
        fieldName = key;
        valueNode = entry.getValue();
      } else {
        throwBadRequestException(String.format("Field '%s' is not valid", fieldName));
      }
    }

    if (valueNode == null) {
      throwBadRequestException(String.format("Value of field '%s' must be not null", fieldName));
    }

    try {
      Field field = parser.getQueryField(null, fieldName);
      value = Long.parseLong(field.asValue(valueNode).toString());
    } catch (BadRequestException ex) {
      throw new BadRequestException(
          String.format(CREATION_FAILED, this.name()),
          String.format(
              "Field '%s' with type Long and value '%s' of type '%s' mismatch",
              fieldName,
              valueNode.asText(),
              valueNode.getClass().getSimpleName().toLowerCase().replace("node", "")));
    }
  }

  @Override
  public Long getUnitUp() {
    return value;
  }

  private void throwBadRequestException(String message) {
    throw new BadRequestException(String.format(CREATION_FAILED, this.name()), message);
  }
}
