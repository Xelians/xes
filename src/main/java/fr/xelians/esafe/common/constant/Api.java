/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constant;

/*
 * @author Emmanuel Deviller
 */
public final class Api {

  public static final String V1 = "/v1";
  public static final String V2 = "/v2";

  // Authentication
  public static final String SIGNUP = "/signup";

  // Organization
  public static final String ORGANIZATIONS = "/organizations";
  public static final String TENANTS = "/tenants";
  public static final String USERS = "/users";
  public static final String ME = "/me";

  // Ingest
  public static final String INGEST_EXTERNAL = "/ingest-external";
  public static final String INGESTS = "/ingests";
  public static final String ATR_XML = "/ingests/{operationId}/archivetransferreply";
  public static final String ATR_JSON = "/ingests/{operationId}/atr";
  public static final String MANIFESTS = "/ingests/{operationId}/manifests";

  // Access
  public static final String ACCESS_EXTERNAL = "/access-external";

  public static final String UNITS = "/units";
  public static final String UNITS_RULES = "/units/rules";
  public static final String UNITS_STREAM = "/units/stream";
  public static final String UNITS_WITH_INHERITED_RULES = "/unitsWithInheritedRules";

  public static final String RECLASSIFICATION = "/reclassification";
  public static final String ELIMINATION_ANALYSIS = "/elimination/analysis";
  public static final String ELIMINATION_ACTION = "/elimination/action";
  public static final String EXPORT = "/dipexport";
  public static final String TRANSFER = "/transfers";
  public static final String TRANSFER_REPLY = "/transfers/reply";
  public static final String PROBATIVE_VALUE_EXPORT = "/probativevalueexport";

  public static final String OBJECTS = "/objects";
  public static final String DOWNLOAD = "/download";
  public static final String DOWNLOADABLE_IDS = "/downloadableids";

  // Logbook
  public static final String LOGBOOK_EXTERNAL = "/access-external";
  public static final String LOGBOOK_OPERATIONS = "/logbookoperations";
  public static final String LOGBOOK_OPERATIONS_SEARCH = "/logbookoperations/search";
  public static final String LOGBOOK_UNIT_LIFECYCLES = "/logbookunitlifecycles/{unitId}";
  public static final String LOGBOOK_OBJECT_LIFECYCLES = "/logbookobjectlifecycles/{unitId}";

  // Admin
  public static final String ADMIN_EXTERNAL = "/admin-external";

  // Accession register (Admin)
  public static final String ACCESSION_REGISTERS = "/accessionregisters";
  public static final String ACCESSION_REGISTER_SUMMARY = "/accessionregisters";
  public static final String ACCESSION_REGISTER_SYMBOLIC = "/accessionregisterssymbolic";
  public static final String ACCESSION_REGISTER_DETAILS = "/accessionregisterdetails";

  // Referential (Admin)
  public static final String ONTOLOGIES = "/ontologies";
  public static final String AGENCIES = "/agencies";
  public static final String RULES = "/rules";
  public static final String RULES_REPORT = "/rulesreport";
  public static final String PROFILES = "/profiles";
  public static final String INGEST_CONTRACTS = "/ingestcontracts";
  public static final String ACCESS_CONTRACTS = "/accesscontracts";

  // Operation (Admin)
  public static final String OPERATIONS = "/operations";
  public static final String OPERATIONS_STATUS = "/operations/status";
  public static final String OPERATIONS_ID = "/operations/{operationId}";
  public static final String OPERATIONS_ID_STATUS = "/operations/{operationId}/status";

  // Batch (Admin)
  public static final String BATCH_REPORT = "/batchreport/{operationId}";

  // Admin (Admin)
  public static final String RESET_SEARCH_ENGINE_INDEX = "/admin/index/new";
  public static final String UPDATE_SEARCH_ENGINE_INDEX = "/admin/index/reindex";
  public static final String REBUILD_SEARCH_ENGINE_INDEX = "/admin/index/rebuild";
  public static final String CHECK_COHERENCE = "/admin/coherency/check/{delay}/{duration}";
  public static final String REPAIR_COHERENCE = "/admin/coherency/repair";
  public static final String ADD_STORAGE_OFFER = "/admin/storageoffer/{offer}";

  // User
  public static final String USER_TOKEN = "/access-key";

  private Api() {
    // do nothing
  }
}
