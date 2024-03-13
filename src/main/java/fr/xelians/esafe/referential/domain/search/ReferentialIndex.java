/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.domain.search;

import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.field.*;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.Map;

public class ReferentialIndex implements Searchable {

  public static final String NAME = "Ontology";
  public static final String ALIAS = NAME + "_alias";

  private static final Map<String, Field> STD_FIELDS =
      Map.ofEntries(
          Map.entry("_id", new LongField("id", true)),
          Map.entry("_tenant", new LongField("tenant", true)),
          Map.entry("identifier", new KeywordField("identifier", true)),
          Map.entry("operationId", new LongField("operationId", true)),
          Map.entry("autoVersion", new IntegerField("autoVersion", true)),
          Map.entry("name", new KeywordField("name", true)),
          Map.entry("description", new TextField("description", true)),
          Map.entry("creationDate", new DateField("creationDate", true)),
          Map.entry("lastUpdate", new DateField("lastUpdate", true)),
          Map.entry("activationDate", new DateField("activationDate", true)),
          Map.entry("deactivationDate", new DateField("deactivationDate", true)),
          Map.entry("status", new StatusField("status", true)));

  private static final Map<String, Field> ALIAS_FIELDS = Map.ofEntries();

  public static final ReferentialIndex INSTANCE = new ReferentialIndex();

  private ReferentialIndex() {}

  @Override
  public boolean isSpecialField(String fieldName) {
    return ReferentialSpecialFields.isSpecial(fieldName);
  }

  @Override
  public String getSpecialFieldName(String fieldName, FieldContext context) {
    return ReferentialSpecialFields.getFieldName(fieldName, context);
  }

  @Override
  public String getName() {
    return NAME;
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
