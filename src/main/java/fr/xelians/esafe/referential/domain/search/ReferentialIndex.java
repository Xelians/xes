/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.domain.search;

import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.field.*;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.Map;

public class ReferentialIndex implements Searchable {

  public static final String NAME = "Ontology";
  public static final String ALIAS = NAME + "_alias";

  // Fields in DB cannot start with _
  private static final Map<String, Field> STD_FIELDS =
      Map.ofEntries(
          Map.entry("_id", new LongField("id", true)),
          Map.entry("_tenant", new LongField("tenant", true)),
          Map.entry("Identifier", new KeywordField("identifier", true)),
          Map.entry("OperationId", new LongField("operationId", true)),
          Map.entry("AutoVersion", new IntegerField("autoVersion", true)),
          Map.entry("Name", new KeywordField("name", true)),
          Map.entry("Description", new TextField("description", true)),
          Map.entry("CreationDate", new DateField("creationDate", true)),
          Map.entry("LastUpdate", new DateField("lastUpdate", true)),
          Map.entry("ActivationDate", new DateField("activationDate", true)),
          Map.entry("DeactivationDate", new DateField("deactivationDate", true)),
          Map.entry("Status", new StatusField("status", true)));

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
