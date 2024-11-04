/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.domain;

import lombok.Getter;

/*
 * @author Emmanuel Deviller
 */
@Getter
public enum OperationType {

  // Archive Operations
  INGEST_ARCHIVE(false, "PROCESS_SIP_UNITARY"),
  INGEST_FILING(false, "FILINGSCHEME"),
  INGEST_HOLDING(false, "HOLDINGSCHEME"),

  UPDATE_ARCHIVE(true, "MASS_UPDATE_UNIT_DESC"),
  UPDATE_ARCHIVE_RULES(true, "MASS_UPDATE_UNIT_RULE"),
  RECLASSIFY_ARCHIVE(true, "RECLASSIFICATION"),
  ELIMINATE_ARCHIVE(true, "ELIMINATION_ACTION"),

  PROBATIVE_VALUE(false, "EXPORT_PROBATIVE_VALUE"),
  EXPORT_ARCHIVE(false, "EXPORT_DIP"),
  TRANSFER_ARCHIVE(false, "TRANSFER"),
  TRANSFER_REPLY(false, "TRANSFER_REPLY"),

  TRACEABILITY(false, "STP_OP_SECURISATION"),

  // Search Engine Operations
  REBUILD_SEARCH_ENGINE(false, "REBUILD_SEARCH_ENGINE"),
  RESET_INDEX(false, "RESET_INDEX"),
  RESET_INDEX_CHUNK(false, "RESET_INDEX_CHUNK"),

  // Referential Operations
  CREATE_AGENCY(false, "CREATE_AGENCY"),
  UPDATE_AGENCY(false, "UPDATE_AGENCY"),
  DELETE_AGENCY(false, "DELETE_AGENCY"),
  CREATE_ONTOLOGY(false, "CREATE_ONTOLOGY"),
  UPDATE_ONTOLOGY(false, "UPDATE_ONTOLOGY"),
  DELETE_ONTOLOGY(false, "DELETE_ONTOLOGY"),
  CREATE_ACCESSCONTRACT(false, "CREATE_ACCESSCONTRACT"),
  UPDATE_ACCESSCONTRACT(false, "UPDATE_ACCESSCONTRACT"),
  DELETE_ACCESSCONTRACT(false, "DELETE_ACCESSCONTRACT"),
  CREATE_INGESTCONTRACT(false, "CREATE_INGESTCONTRACT"),
  UPDATE_INGESTCONTRACT(false, "UPDATE_INGESTCONTRACT"),
  DELETE_INGESTCONTRACT(false, "DELETE_INGESTCONTRACT"),
  CREATE_PROFILE(false, "CREATE_PROFILE"),
  UPDATE_PROFILE(false, "UPDATE_PROFILE"),
  DELETE_PROFILE(false, "DELETE_PROFILE"),
  CREATE_RULE(false, "CREATE_RULE"),
  UPDATE_RULE(false, "UPDATE_RULE"),
  DELETE_RULE(false, "DELETE_RULE"),

  // Orga
  CREATE_ROLE(false, "CREATE_ROLE"),
  UPDATE_ROLE(false, "UPDATE_ROLE"),
  CREATE_USER(false, "CREATE_USER"),
  UPDATE_USER(false, "UPDATE_USER"),
  CREATE_ORGANIZATION(false, "CREATE_ORGANIZATION"),
  UPDATE_ORGANIZATION(false, "UPDATE_ORGANIZATION"),
  CREATE_TENANT(false, "CREATE_TENANT"),
  UPDATE_TENANT(false, "UPDATE_TENANT"),

  // External
  EXTERNAL(false, "EXTERNAL"),

  // Offer Operations
  ADD_OFFER(false, "ADD_OFFER"),
  ADD_OFFER_CHUNK(false, "ADD_OFFER_CHUNK"),
  DELETE_OFFER(false, "DELETE_OFFER"),

  // Audit operations
  CHECK_COHERENCY(false, "CHECK_LOGBOOK_OP_SECURISATION"),
  CHECK_TENANT_COHERENCY(false, "CHECK_TENANT_COHERENCY"),

  REPAIR_COHERENCY(false, "REPAIR_COHERENCY"),

  AUDIT(false, "AUDIT"),
  SYNC(false, "SYNC");

  // TODO : a étudier
  //  FULL_EXCLUSIVE : interdit l'ajout de toutes les tasks sur le threadPoolExecutor (ie.
  // RECLASSIFY)
  //  SOFT_EXCLUSIVE : autorise uniquement les NONE_EXCLUSIVE task à être ajoutées sur le thread
  // pool (ie. UPDATE autorise INGEST)
  //  NONE_EXCLUSIVE : autorise toutes les taches à être ajoutées (ie. ie_INGEST)
  private final boolean isExclusive;

  private final String info;

  OperationType(boolean isExclusive, String info) {
    this.isExclusive = isExclusive;
    this.info = info;
  }

  public static boolean isIngest(OperationType type) {
    return type == INGEST_ARCHIVE || type == INGEST_HOLDING || type == INGEST_FILING;
  }
}
