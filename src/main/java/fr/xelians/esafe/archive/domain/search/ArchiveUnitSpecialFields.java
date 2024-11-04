/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search;

import fr.xelians.esafe.common.utils.CollUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import java.util.Map;

/*
 * @author Emmanuel Deviller
 */
public final class ArchiveUnitSpecialFields {

  private static final Map<String, String> BASE_FIELDS = createBaseFields();
  private static final Map<String, String> QUERY_FIELDS = createQueryFields();
  private static final Map<String, String> PROJECTION_FIELDS = createProjectionFields();
  private static final Map<String, String> UPDATE_FIELDS = createUpdateFields();
  private static final Map<String, String> RECLASSIFICATION_FIELDS = createReclassificationFields();

  private ArchiveUnitSpecialFields() {}

  // An archive unit accepts one and only one parent.
  // So #unitup and #unitups refer to the same property.
  private static Map<String, String> createBaseFields() {
    return Map.ofEntries(
        Map.entry("#id", "_unitId"),
        Map.entry("#systemid", "_unitId"),
        Map.entry("#unitType", "_unitType"),
        Map.entry("#unitup", "_up"),
        Map.entry("#unitups", "_up"),
        Map.entry("#allunitups", "_us"),
        Map.entry("#opi", "_opi"),
        Map.entry("#operations", "_ops"),
        Map.entry("#version", "_av"),
        Map.entry("#sedaversion", "_sedaVersion"),
        Map.entry("#implementationVersion", "_implementationVersion"),
        Map.entry("#min", "_min"),
        Map.entry("#max", "_max"),
        Map.entry("#creation", "_creationDate"),
        Map.entry("#update", "_updateDate"),
        Map.entry("#approximate_creation_date", "_creationDate"),
        Map.entry("#approximate_update_date", "_updateDate"),
        Map.entry("#originating_agency", "_sp"),
        Map.entry("#originating_agencies", "_sps"),
        Map.entry("#lifecycles", "_lifeCycles"),
        Map.entry("#lfc_date", "_lifeCycles._opDate"),
        Map.entry("#lfc_opi", "_lifeCycles._opi"),
        Map.entry("#lfc_patch", "_lifeCycles._patch"),
        Map.entry("#lfc_type", "_lifeCycles._opType"),
        Map.entry("#lfc_version", "_lifeCycles._av"),
        Map.entry("#management", "_mgt"),
        Map.entry("#computedInheritedRules", "_cir"),
        Map.entry("#validComputedInheritedRules", "_validCir"),
        Map.entry("#nbobjects", "_nbObjects"),
        Map.entry("#object", "_objectId"),
        Map.entry("#object_id", "_qualifiers.versions._id"),
        Map.entry("#object_opi", "_qualifiers.versions._opi"),
        Map.entry("#size", "_qualifiers.versions.Size"),
        Map.entry("#qualifiers", "_qualifiers"),
        Map.entry("#format", "_qualifiers.versions.FormatIdentification.FormatId"),
        Map.entry("#mimetype", "_qualifiers.versions.FormatIdentification.MimeType"),
        Map.entry("#type", "DocumentType"),
        Map.entry("#full_search", "_fullSearch"),
        Map.entry("#score", "_score"),
        Map.entry("#transferred", "_transferred"));
  }

  // Query supported special fields - Unsupported fields : #nbunits
  private static Map<String, String> createQueryFields() {
    return CollUtils.concatMap(
        BASE_FIELDS,
        Map.of("#keywords", "_keywords", "#fullsearch", "_fullSearch", "#fulltext", "_fulltext"));
  }

  // Projection supported special fields - Unsupported fields : #nbunits, #score, #storage
  private static Map<String, String> createProjectionFields() {
    return CollUtils.concatMap(BASE_FIELDS, Map.of("#tenant", "_tenant"));
  }

  // Update supported special fields
  private static Map<String, String> createUpdateFields() {
    return Map.of("#management", "_mgt");
  }

  // Reclassification supported special fields
  private static Map<String, String> createReclassificationFields() {
    return Map.of("#unitup", "_up", "#unitups", "_up");
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
