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
import java.util.Map;

/*
 * @author Emmanuel Deviller
 */
public class RegisterDetailsIndex implements Searchable {

  public static final String MAPPING =
      """
                    {
                      "dynamic": "strict",
                      "properties": {
                        "_detailsId": {
                          "type": "keyword"
                        },
                        "_tenant": {
                          "type": "long"
                        },
                        "_v": {
                          "type": "long"
                        },
                        "OriginatingAgency": {
                          "type": "keyword"
                        },
                        "SubmissionAgency": {
                          "type": "keyword"
                        },
                        "ArchivalAgreement": {
                          "type": "keyword"
                        },
                        "ArchivalProfile": {
                          "type": "keyword"
                        },
                        "OperationIds": {
                          "type": "keyword"
                        },
                        "OpType": {
                          "type": "keyword"
                        },
                        "LegalStatus": {
                          "type": "keyword"
                        },
                        "obIdIn": {
                          "type": "text"
                        },
                        "Comments": {
                          "type": "text"
                        },
                        "AcquisitionInformation": {
                          "type": "text"
                        },
                        "Opc": {
                          "type": "keyword"
                        },
                        "Opi": {
                          "type": "keyword"
                        },
                        "EndDate": {
                          "type": "date",
                          "format": "strict_date_optional_time"
                        },
                        "StartDate": {
                          "type": "date",
                          "format": "strict_date_optional_time"
                        },
                        "LastUpdate": {
                          "type": "date",
                          "format": "strict_date_optional_time"
                        },
                        "Status": {
                          "type": "keyword"
                        },
                        "Events": {
                          "properties": {
                            "Opc": {
                              "type": "keyword"
                            },
                            "Gots": {
                              "type": "long"
                            },
                            "Units": {
                              "type": "long"
                            },
                            "Objects": {
                              "type": "long"
                            },
                            "ObjSize": {
                              "type": "long"
                            },
                            "OpType": {
                              "type": "keyword"
                            },
                            "CreationDate": {
                              "type": "date",
                              "format": "strict_date_optional_time"
                            }
                          }
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

  public static final String NAME = "registerdetails";
  public static final String ALIAS = NAME + "_alias";

  private static final Map<String, Field> STD_FIELDS = FieldUtils.buildStandardFields(MAPPING);

  private static final Map<String, Field> ALIAS_FIELDS =
      Map.ofEntries(Map.entry("eventId", STD_FIELDS.get("Opi")));

  public static final RegisterDetailsIndex INSTANCE = new RegisterDetailsIndex();

  private RegisterDetailsIndex() {}

  @Override
  public boolean isSpecialField(String fieldName) {
    return RegisterDetailsSpecialFields.isSpecial(fieldName);
  }

  @Override
  public String getSpecialFieldName(String fieldName, FieldContext context) {
    return RegisterDetailsSpecialFields.getFieldName(fieldName, context);
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
