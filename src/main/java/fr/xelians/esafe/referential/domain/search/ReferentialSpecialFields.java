/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.domain.search;

import fr.xelians.esafe.common.utils.CollUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import java.util.Collections;
import java.util.Map;

public final class ReferentialSpecialFields {

  private static final Map<String, String> BASE_FIELDS = createBaseFields();
  private static final Map<String, String> QUERY_FIELDS = createQueryFields();
  private static final Map<String, String> PROJECTION_FIELDS = createProjectionFields();
  private static final Map<String, String> UPDATE_FIELDS = createUpdateFields();
  private static final Map<String, String> RECLASSIFICATION_FIELDS = createReclassificationFields();

  private ReferentialSpecialFields() {}

  private static Map<String, String> createBaseFields() {
    return Collections.emptyMap();
  }

  // Unsupported fields : #nbunits, #nbobjects
  private static Map<String, String> createQueryFields() {
    return Collections.emptyMap();
  }

  // Query supported fields
  // Vitam unsupported fields : #nbunits, #nbobjects, #score, #storage
  private static Map<String, String> createProjectionFields() {
    return CollUtils.concatMap(BASE_FIELDS, Map.of("#tenant", "tenant"));
  }

  // Update supported fields
  private static Map<String, String> createUpdateFields() {
    return Collections.emptyMap();
  }

  // Reclassification  supported fields
  // An archive unit accepts one and only one parent.
  // So #unitup and #unitups refer to the same property.
  private static Map<String, String> createReclassificationFields() {
    return Collections.emptyMap();
  }

  public static boolean isSpecial(String fieldName) {
    return fieldName.startsWith("#");
  }

  public static String getFieldName(String name, FieldContext context) {
    return (switch (context) {
          case QUERY -> QUERY_FIELDS;
          case PROJECTION -> PROJECTION_FIELDS;
          case UPDATE -> UPDATE_FIELDS;
          case RECLASSIFICATION -> RECLASSIFICATION_FIELDS;
        })
        .get(name);
  }
}
