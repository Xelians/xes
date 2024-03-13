/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.logbook.domain.search;

import fr.xelians.esafe.common.utils.FieldUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.field.Field;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.Map;

public class LogbookIndex implements Searchable {

  public static final String MAPPING =
      """
                    {
                      "dynamic": "strict",
                      "properties": {
                        "Created": {
                          "type": "date"
                        },
                        "_operationId": {
                          "type": "long"
                        },
                        "_secureNumber": {
                          "type": "long"
                        },
                        "Modified": {
                          "type": "date"
                        },
                        "_tenant": {
                          "type": "long"
                        },
                        "Type": {
                          "type": "keyword"
                        },
                        "TypeInfo": {
                          "type": "keyword"
                        },
                        "UserIdentifier": {
                          "type": "keyword"
                        },
                        "ApplicationId": {
                          "type": "keyword"
                        },
                        "ObjectIdentifier": {
                          "type": "keyword"
                        },
                        "ObjectInfo": {
                          "type": "keyword"
                        },
                        "ObjectData": {
                          "type": "keyword"
                        },
                        "Outcome": {
                          "type": "keyword"
                        },
                        "Message": {
                          "type": "text"
                        }
                      }
                    }
                    """;

  public static final String INDEX = "logbook";
  public static final String ALIAS = INDEX + "_alias";

  private static final Map<String, Field> STD_FIELDS = FieldUtils.buildStandardFields(MAPPING);

  private static final Map<String, Field> ALIAS_FIELDS =
      Map.ofEntries(
          Map.entry("eventId", STD_FIELDS.get("_operationId")),
          Map.entry("evType", STD_FIELDS.get("TypeInfo")),
          Map.entry("evTypeProc", STD_FIELDS.get("Type")),
          Map.entry("agId", STD_FIELDS.get("UserIdentifier")),
          Map.entry("agIdApp", STD_FIELDS.get("ApplicationId")),
          Map.entry("evIdAppSession", STD_FIELDS.get("ApplicationId")),
          Map.entry("_lastPersistedDate", STD_FIELDS.get("Modified")),
          Map.entry("evDateTime", STD_FIELDS.get("Created")),
          Map.entry("outMessg", STD_FIELDS.get("Message")),
          Map.entry("evDetData", STD_FIELDS.get("ObjectData")),
          Map.entry("obIdReq", STD_FIELDS.get("ObjectInfo")),
          Map.entry("obId", STD_FIELDS.get("ObjectIdentifier")));

  public static final LogbookIndex INSTANCE = new LogbookIndex();

  private LogbookIndex() {}

  @Override
  public boolean isSpecialField(String fieldName) {
    return LogbookSpecialFields.isSpecial(fieldName);
  }

  @Override
  public String getSpecialFieldName(String fieldName, FieldContext context) {
    return LogbookSpecialFields.getFieldName(fieldName, context);
  }

  @Override
  public String getName() {
    return INDEX;
  }

  @Override
  public String getAlias() {
    return ALIAS;
  }

  @Override
  public Field getField(String fieldName) {
    return STD_FIELDS.get(fieldName);
  }

  @Override
  public boolean isExtField(String fieldName) {
    return false;
  }

  @Override
  public boolean isStdField(String fieldName) {
    return STD_FIELDS.containsKey(fieldName);
  }

  @Override
  public Field getAliasField(String fieldName, FieldContext fieldContext) {
    return ALIAS_FIELDS.get(fieldName);
  }
}
