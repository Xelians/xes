/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search;

import fr.xelians.esafe.common.utils.FieldUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.field.*;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * @author Emmanuel Deviller
 */
public final class ArchiveUnitIndex implements Searchable {

  public static final int UPS_SIZE = 10;
  public static final String NAME = "archiveunit";
  public static final String ALIAS = NAME + "_alias";

  private static final Map<String, Field> EXT_FIELDS = buildExtFields();
  public static final String MAPPING = buildArchiveUnitMapping();
  private static final Map<String, Field> STD_FIELDS = FieldUtils.buildStandardFields(MAPPING);

  private static final Map<String, Field> FIELDS = new HashMap<>();
  private static final Set<String> FIELD_NAMES = new HashSet<>();

  static {
    FIELDS.putAll(STD_FIELDS);
    FIELDS.putAll(EXT_FIELDS);
    FIELDS.keySet().stream().map(String::toLowerCase).forEach(FIELD_NAMES::add);
  }

  private static final Map<String, Field> ALIAS_FIELDS =
      Map.ofEntries(
          Map.entry("full_search", STD_FIELDS.get("_fullSearch")),
          Map.entry("Title_", STD_FIELDS.get("Title_")),
          Map.entry("Title_.fr", STD_FIELDS.get("Title_.fr")),
          Map.entry("Title_.en", STD_FIELDS.get("Title_.en")),
          Map.entry("DocumentType_keyword", STD_FIELDS.get("DocumentType")),
          Map.entry("FileInfo.Filename", STD_FIELDS.get("_qualifiers.versions.FileInfo.Filename")),
          Map.entry(
              "FileInfo.LastModified",
              STD_FIELDS.get("_qualifiers.versions.FileInfo.LastModified")),
          Map.entry(
              "FileInfo.CreatingApplicationName",
              STD_FIELDS.get("_qualifiers.versions.FileInfo.CreatingApplicationName")),
          Map.entry(
              "FileInfo.CreatingApplicationVersion",
              STD_FIELDS.get("_qualifiers.versions.FileInfo.CreatingApplicationVersion")),
          Map.entry(
              "FileInfo.CreatingOs", STD_FIELDS.get("_qualifiers.versions.FileInfo.CreatingOs")),
          Map.entry(
              "FileInfo.CreatingOsVersion",
              STD_FIELDS.get("_qualifiers.versions.FileInfo.CreatingOsVersion")),
          Map.entry(
              "FileInfo.DateCreatedByApplication",
              STD_FIELDS.get("_qualifiers.versions.FileInfo.DateCreatedByApplication")));

  public static final ArchiveUnitIndex INSTANCE = new ArchiveUnitIndex();

  private ArchiveUnitIndex() {}

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
    return FIELDS.get(fieldName);
  }

  @Override
  public boolean isExtField(String fieldName) {
    return EXT_FIELDS.containsKey(fieldName);
  }

  @Override
  public boolean isStdField(String fieldName) {
    return STD_FIELDS.containsKey(fieldName);
  }

  @Override
  public boolean isSpecialField(String fieldName) {
    return ArchiveUnitSpecialFields.isSpecial(fieldName);
  }

  @Override
  public String getSpecialFieldName(String fieldName, FieldContext context) {
    return ArchiveUnitSpecialFields.getFieldName(fieldName, context);
  }

  @Override
  public Field getAliasField(String fieldName, FieldContext fieldContext) {
    return ALIAS_FIELDS.get(fieldName);
  }

  // This function is case-insensitive
  public static boolean containsFieldName(String fieldName) {
    return FIELD_NAMES.contains(fieldName.toLowerCase());
  }

  // Fields like TEXT001, KEY002, DATE001, LONG043, DOUBLE009
  private static Map<String, Field> buildExtFields() {
    Map<String, Field> fields = new HashMap<>();
    IntStream.rangeClosed(1, KeywordField.SIZE)
        .forEach(i -> fields.put(KeywordField.getFieldName(i), new KeywordField(i, false)));
    IntStream.rangeClosed(1, TextField.SIZE)
        .forEach(i -> fields.put(TextField.getFieldName(i), new TextField(i, false)));
    IntStream.rangeClosed(1, DateField.SIZE)
        .forEach(i -> fields.put(DateField.getFieldName(i), new DateField(i, false)));
    IntStream.rangeClosed(1, LongField.SIZE)
        .forEach(i -> fields.put(LongField.getFieldName(i), new LongField(i, false)));
    IntStream.rangeClosed(1, DoubleField.SIZE)
        .forEach(i -> fields.put(DoubleField.getFieldName(i), new DoubleField(i, false)));
    return fields;
  }

  // TODO implementer le full text - sinon utiliser la classe mapping fournie par Elastic
  // type and properties are not allowed property (but Type and Properties are allowed)
  // Default date format is "strict_date_optional_time||epoch_millis"

  // FullText should be _fullText
  // full_search should be _fullSearch
  private static String buildArchiveUnitMapping() {
    return """
         {
           "dynamic": "strict",
           "properties": {
             "_av": {
               "type": "integer"
             },
             "_min": {
               "type": "integer"
             },
             "_max": {
               "type": "integer"
             },
             "_creationDate": {
               "type": "date"
             },
             "_updateDate": {
               "type": "date"
             },
             "_sedaVersion": {
              "type": "keyword"
             },
             "_implementationVersion": {
              "type": "keyword"
             },
             "_fullSearch": {
               "type": "text"
             },
             "FullText": {
               "type": "text"
             },
             "_transferred": {
               "type": "boolean"
             },
             "_lifeCycles": {
               "properties": {
                 "_av": {
                   "type": "integer"
                 },
                 "_opDate": {
                   "type": "date"
                 },
                 "_opi": {
                   "type": "keyword"
                 },
                 "_opType": {
                   "type": "text"
                 },
                 "_patch": {
                   "type": "text"
                 }
               }
             },
             "_mgt": {
               "properties": {
                 "OriginatingAgencyIdentifier": {
                  "type": "keyword"
                 },
                 "SubmissionAgencyIdentifier": {
                  "type": "keyword"
                 },
                 "AccessRule": {
                   "properties": {
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "AppraisalRule": {
                   "properties": {
                     "Duration": {
                       "type": "keyword"
                     },
                     "FinalAction": {
                       "type": "keyword"
                     },
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "ClassificationRule": {
                   "properties": {
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "ClassificationLevel": {
                       "type": "keyword"
                     },
                     "ClassificationAudience": {
                       "type": "keyword"
                     },
                     "ClassificationOwner": {
                       "type": "keyword"
                     },
                     "ClassificationReassessingDate": {
                       "type": "date"
                     },
                     "NeedReassessingAuthorization": {
                       "type": "boolean"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "DisseminationRule": {
                   "properties": {
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "ReuseRule": {
                   "properties": {
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "StorageRule": {
                   "properties": {
                     "FinalAction": {
                       "type": "keyword"
                     },
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "HoldRule": {
                   "properties": {
                     "EndDate": {
                       "type": "date"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "Inheritance": {
                       "properties": {
                         "PreventInheritance": {
                           "type": "boolean"
                         },
                         "PreventRulesId": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "StartDate": {
                           "type": "date"
                         },
                         "EndDate": {
                           "type": "date"
                         },
                         "HoldEndDate": {
                           "type": "date"
                         },
                         "HoldOwner": {
                           "type": "text"
                         },
                         "HoldReason": {
                           "type": "text"
                         },
                         "HoldReassessingDate": {
                           "type": "date"
                         },
                         "PreventRearrangement": {
                           "type": "boolean"
                         }
                       }
                     }
                   }
                 }
               }
             },
             "_validCir": {
               "type": "boolean"
             },
             "_cir": {
               "properties": {
                 "AccessRule": {
                   "properties": {
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "AppraisalRule": {
                   "properties": {
                     "FinalAction": {
                       "type": "keyword"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "ClassificationRule": {
                   "properties": {
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     },
                     "ClassificationLevel": {
                       "type": "keyword"
                     },
                     "ClassificationAudience": {
                       "type": "keyword"
                     },
                     "ClassificationOwner": {
                       "type": "keyword"
                     },
                     "ClassificationReassessingDate": {
                       "type": "date"
                     },
                     "NeedReassessingAuthorization": {
                       "type": "boolean"
                     }
                   }
                 },
                 "DisseminationRule": {
                   "properties": {
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "ReuseRule": {
                   "properties": {
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "StorageRule": {
                   "properties": {
                     "FinalAction": {
                       "type": "keyword"
                     },
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     }
                   }
                 },
                 "HoldRule": {
                   "properties": {
                     "MaxEndDate": {
                       "type": "date"
                     },
                     "InheritanceOrigin": {
                       "type": "keyword"
                     },
                     "Rules": {
                       "properties": {
                         "Rule": {
                           "type": "keyword"
                         },
                         "EndDate": {
                           "type": "date"
                         }
                       }
                     },
                     "PreventRearrangement": {
                       "type": "boolean"
                     }
                   }
                 }
               }
             },
             "_opi": {
               "type": "keyword"
             },
             "_ops": {
               "type": "keyword"
             },
             "_sp": {
               "type": "keyword"
             },
             "_sps": {
               "type": "keyword"
             },
             "_tenant": {
               "type": "long"
             },
             "_unitId": {
               "type": "keyword"
             },
             "_unitType": {
               "type": "keyword"
             },
             "_up": {
               "type": "keyword"
             },
             "_us": {
               "type": "keyword"
             },
             "_objectId": {
               "type": "keyword"
             },
             "AcquiredDate": {
               "type": "date"
             },
             "ArchivalAgencyArchiveUnitIdentifier": {
               "type": "keyword"
             },
             "ArchiveUnitProfile": {
               "type": "keyword"
             },
             "_nbObjects": {
               "type": "integer"
             },
             "_qualifiers": {
               "properties": {
                 "qualifier": {
                   "type": "keyword"
                 },
                 "_nbc": {
                   "type": "integer"
                 },
                 "versions": {
                   "properties": {
                     "_id": {
                       "type": "keyword"
                     },
                     "PhysicalId": {
                       "type": "keyword"
                     },
                     "Measure": {
                       "type": "double"
                     },
                     "DataObjectVersion": {
                       "type": "keyword"
                     },
                     "FileInfo": {
                       "properties": {
                         "CreatingApplicationName": {
                           "type": "keyword"
                         },
                         "CreatingApplicationVersion": {
                           "type": "keyword"
                         },
                         "CreatingOs": {
                           "type": "keyword"
                         },
                         "CreatingOsVersion": {
                           "type": "keyword"
                         },
                         "DateCreatedByApplication": {
                           "type": "date"
                         },
                         "Filename": {
                           "type": "keyword"
                         },
                         "LastModified": {
                           "type": "date"
                         }
                       }
                     },
                     "FormatIdentification": {
                       "properties": {
                         "Encoding": {
                           "type": "keyword"
                         },
                         "FormatId": {
                           "type": "keyword"
                         },
                         "FormatLitteral": {
                           "type": "keyword"
                         },
                         "FormatName": {
                           "type": "keyword"
                         },
                         "MimeType": {
                           "type": "keyword"
                         }
                       }
                     },
                     "Algorithm": {
                       "type": "keyword"
                     },
                     "MessageDigest": {
                       "type": "keyword"
                     },
                     "Size": {
                       "type": "long"
                     },
                     "_opi": {
                       "type": "keyword"
                     },
                     "_pos": {
                       "type": "long"
                     }
                   }
                 }
               }
             },
             "DataObject": {
               "properties": {
                 "BinaryDataObjects": {
                   "properties": {
                     "_binaryId": {
                       "type": "keyword"
                     },
                     "_opi": {
                       "type": "keyword"
                     },
                     "_pos": {
                       "type": "long"
                     },
                     "BinaryVersion": {
                       "type": "keyword"
                     },
                     "DigestAlgorithm": {
                       "type": "keyword"
                     },
                     "FileInfo": {
                       "properties": {
                         "CreatingApplicationName": {
                           "type": "keyword"
                         },
                         "CreatingApplicationVersion": {
                           "type": "keyword"
                         },
                         "CreatingOs": {
                           "type": "keyword"
                         },
                         "CreatingOsVersion": {
                           "type": "keyword"
                         },
                         "DateCreatedByApplication": {
                           "type": "date"
                         },
                         "Filename": {
                           "type": "keyword"
                         },
                         "LastModified": {
                           "type": "date"
                         }
                       }
                     },
                     "FormatIdentification": {
                       "properties": {
                         "Encoding": {
                           "type": "keyword"
                         },
                         "FormatId": {
                           "type": "keyword"
                         },
                         "FormatLitteral": {
                           "type": "keyword"
                         },
                         "FormatName": {
                           "type": "keyword"
                         },
                         "MimeType": {
                           "type": "keyword"
                         }
                       }
                     },
                     "MessageDigest": {
                       "type": "keyword"
                     },
                     "Size": {
                       "type": "long"
                     }
                   }
                 },
                 "PhysicalDataObjects": {
                   "properties": {
                     "PhysicalId": {
                       "type": "keyword"
                     },
                     "PhysicalVersion": {
                       "type": "keyword"
                     },
                     "Measure": {
                       "type": "double"
                     }
                   }
                 }
              }
             },
             "CreatedDate": {
               "type": "date"
             },
             "CustodialHistoryItems": {
               "type": "text"
             },
             "DescriptionLevel": {
               "type": "keyword"
             },
             "Description": {
               "type": "text",
               "copy_to": [
                 "_fullSearch"
               ]
             },
             "Type": {
               "type": "keyword"
             },
             "DocumentType": {
               "type": "keyword"
             },
             "EndDate": {
               "type": "date"
             },
             "_extents": {
               "type": "object",
               "enabled": false
             },
             "FilePlanPosition": {
               "type": "keyword"
             },
             "Keyword": {
               "properties": {
                 "KeywordReference": {
                   "type": "keyword"
                 },
                 "KeywordContent": {
                   "type": "keyword"
                 }
               }
             },
             "_keywords": {
               "type": "keyword"
             },
             "OriginatingAgencyArchiveUnitIdentifier": {
               "type": "keyword"
             },
             "OriginatingSystemId": {
               "type": "keyword"
             },
             "ReceivedDate": {
               "type": "date"
             },
             "RegisteredDate": {
               "type": "date"
             },
             "SentDate": {
               "type": "date"
             },
             "StartDate": {
               "type": "date"
             },
             "Status": {
               "type": "keyword"
             },
             "Tags": {
               "type": "keyword"
             },
             "Title": {
               "type": "text",
               "fielddata": true,
               "fields": {
                 "keyword": {
                   "type": "keyword",
                   "normalizer": "lowercase"
                 }
               },
               "copy_to": [
                 "_fullSearch"
               ]
             },
             "Title_": {
               "properties": {
                 "fr": {
                   "type": "text",
                   "copy_to": [
                     "_fullSearch"
                   ]
                 },
                 "en": {
                   "type": "text",
                   "copy_to": [
                     "_fullSearch"
                   ]
                 }
               }
             },
             "TransactedDate": {
               "type": "date"
             },
             "TransferringAgencyArchiveUnitIdentifier": {
               "type": "keyword"
             },
             "Version": {
               "type": "keyword"
             },
             "_ups": {
               "properties": {
         """
        + upsFields()
        + """
               }
             },
             "_ext": {
               "properties": {
         """
        + extFields()
        + "}}}}";
  }

  private static String upsFields() {
    String jsonUpsField = """
                "_up%d": {"type": "keyword"}
                """;

    return IntStream.rangeClosed(2, UPS_SIZE)
        .mapToObj(i -> String.format(jsonUpsField, i))
        .collect(Collectors.joining(", "));
  }

  private static String extFields() {
    String jsonExtField =
        """
                 "%s": {
                   "type": "%s",
                   "copy_to": [
                     "_fullSearch"
                   ]}
                """;

    return EXT_FIELDS.values().stream()
        .map(field -> String.format(jsonExtField, field.getName(), field.getType()))
        .collect(Collectors.joining(", "));
  }
}
