/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.domain;

import lombok.Getter;

@Getter
public enum OperationType {

  // Archive Operations
  INGEST_ARCHIVE(false),
  INGEST_FILING(false),
  INGEST_HOLDING(false),

  UPDATE_ARCHIVE(true),
  UPDATE_ARCHIVE_RULES(true),
  RECLASSIFY_ARCHIVE(true),
  ELIMINATE_ARCHIVE(true),

  PROBATIVE_VALUE(false),
  EXPORT_ARCHIVE(false),
  TRANSFER_ARCHIVE(false),

  SECURING(false),

  // Search Engine Operations
  REBUILD_SEARCH_ENGINE(false),
  RESET_INDEX(false),
  RESET_INDEX_CHUNK(false),

  // Referential Operations
  CREATE_AGENCY(false),
  UPDATE_AGENCY(false),
  CREATE_ONTOLOGY(false),
  UPDATE_ONTOLOGY(false),
  CREATE_ACCESSCONTRACT(false),
  UPDATE_ACCESSCONTRACT(false),
  CREATE_INGESTCONTRACT(false),
  UPDATE_INGESTCONTRACT(false),
  CREATE_PROFILE(false),
  UPDATE_PROFILE(false),
  CREATE_RULE(false),
  UPDATE_RULE(false),

  // Orga
  CREATE_ROLE(false),
  UPDATE_ROLE(false),
  CREATE_USER(false),
  UPDATE_USER(false),
  CREATE_ORGANIZATION(false),
  UPDATE_ORGANIZATION(false),
  CREATE_TENANT(false),
  UPDATE_TENANT(false),

  // External
  EXTERNAL(false),

  // Offer Operations
  ADD_OFFER(false),
  ADD_OFFER_CHUNK(false),
  DELETE_OFFER(false),

  // Audit operations
  CHECK_COHERENCY(false),
  CHECK_TENANT_COHERENCY(false),

  REPAIR_COHERENCY(false),

  AUDIT(false),
  SYNC(false);

  // TODO : a étudier
  //  FULL_EXCLUSIVE : interdit l'ajout de toutes les tasks sur le threadPoolExecutor (ie.
  // RECLASSIFY)
  //  SOFT_EXCLUSIVE : autorise uniquement les NONE_EXCLUSIVE task à être ajoutées sur le thread
  // pool (ie. UPDATE autorise INGEST)
  //  NONE_EXCLUSIVE : autorise toutes les taches à être ajoutées (ie. ie_INGEST)
  private final boolean isExclusive;

  OperationType(boolean isExclusive) {
    this.isExclusive = isExclusive;
  }
}
