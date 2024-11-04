/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.domain.search;

import fr.xelians.esafe.common.utils.FieldUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.field.Field;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.Collections;
import java.util.Map;

/*
 * @author Emmanuel Deviller
 */
public abstract class RegisterIndex implements Searchable {

  public static final String MAPPING =
      """
                    {
                      "dynamic": "strict",
                      "properties": {
                        "_registerId": {
                          "type": "keyword"
                        },
                        "_tenant": {
                          "type": "long"
                        },
                        "_v": {
                          "type": "long"
                        },
                        "OperationIds": {
                          "type": "keyword"
                        },
                        "OriginatingAgency": {
                          "type": "keyword"
                        },
                        "CreationDate": {
                          "type": "date",
                          "format": "strict_date_optional_time"
                        },
                        "TotalObjectGroups": {
                          "properties": {
                            "ingested": {
                              "type": "long"
                            },
                            "deleted": {
                              "type": "long"
                            },
                            "remained": {
                              "type": "long"
                            }
                          }
                        },
                        "TotalUnits": {
                          "properties": {
                            "ingested": {
                              "type": "long"
                            },
                            "deleted": {
                              "type": "long"
                            },
                            "remained": {
                              "type": "long"
                            }
                          }
                        },
                        "TotalObjects": {
                          "properties": {
                            "ingested": {
                              "type": "long"
                            },
                            "deleted": {
                              "type": "long"
                            },
                            "remained": {
                              "type": "long"
                            }
                          }
                        },
                        "ObjectSize": {
                          "properties": {
                            "ingested": {
                              "type": "long"
                            },
                            "deleted": {
                              "type": "long"
                            },
                            "remained": {
                              "type": "long"
                            }
                          }
                        }
                      }
                    }
                    """;

  private static final Map<String, Field> STD_FIELDS = FieldUtils.buildStandardFields(MAPPING);
  private static final Map<String, Field> ALIAS_FIELDS = Collections.emptyMap();

  @Override
  public boolean isSpecialField(String fieldName) {
    return RegisterSpecialFields.isSpecial(fieldName);
  }

  @Override
  public String getSpecialFieldName(String fieldName, FieldContext context) {
    return RegisterSpecialFields.getFieldName(fieldName, context);
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
