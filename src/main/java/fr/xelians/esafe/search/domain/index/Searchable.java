/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.index;

import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.field.Field;

public interface Searchable {

  String getName();

  String getAlias();

  Field getField(String fieldName);

  boolean isExtField(String fieldName);

  boolean isStdField(String fieldName);

  boolean isSpecialField(String fieldName);

  String getSpecialFieldName(String fieldName, FieldContext type);

  Field getAliasField(String fieldName, FieldContext fieldContext);
}
