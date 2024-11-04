/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.domain;

import static fr.xelians.esafe.operation.domain.OperationType.*;

import fr.xelians.esafe.archive.domain.ingest.ContextId;
import fr.xelians.esafe.common.utils.OperationUtils;
import fr.xelians.esafe.operation.entity.OperationDb;

/*
 * @author Emmanuel Deviller
 */
public final class OperationFactory {
  private OperationFactory() {}

  public static OperationDb resetIndexOp(Long tenant, String user, String app) {
    return new OperationDb(RESET_INDEX, tenant, user, app, null);
  }

  public static OperationDb resetTenantIndexOp(Long tenant, String user, String app) {
    return new OperationDb(RESET_INDEX_CHUNK, tenant, user, app, null);
  }

  public static OperationDb rebuildIndexOp(Long tenant, String user, String app) {
    return new OperationDb(REBUILD_SEARCH_ENGINE, tenant, user, app, null);
  }

  public static OperationDb addOfferOp(Long tenant, String user, String app, String dstOffer) {
    OperationDb ope = new OperationDb(ADD_OFFER, tenant, user, app, null);
    ope.setProperty01(dstOffer);
    return ope;
  }

  public static OperationDb checkCoherencyOp(
      Long tenant, String user, String app, int delay, int duration) {
    OperationDb ope = new OperationDb(CHECK_COHERENCY, tenant, user, app, null);
    ope.setProperty01(String.valueOf(delay));
    ope.setProperty02(String.valueOf(duration));
    return ope;
  }

  public static OperationDb checkTenantCoherencyOp(Long tenant, String user, String app) {
    return new OperationDb(CHECK_TENANT_COHERENCY, tenant, user, app, null);
  }

  public static OperationDb ingestArchiveOp(
      Long tenant, ContextId contextId, String user, String app) {
    OperationType opType = OperationUtils.getIngestOperationType(contextId);
    return new OperationDb(opType, tenant, user, app, null, true, true);
  }

  public static OperationDb updateArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(UPDATE_ARCHIVE, tenant, user, app, contract, true);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb updateArchiveRulesOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(UPDATE_ARCHIVE_RULES, tenant, user, app, contract, true);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb transferArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(TRANSFER_ARCHIVE, tenant, user, app, contract, true);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb transferReplyArchiveOp(
      Long tenant, String contract, String user, String app) {
    return new OperationDb(TRANSFER_ARCHIVE, tenant, user, app, contract, true, true);
  }

  public static OperationDb probativeValueOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(PROBATIVE_VALUE, tenant, user, app, contract, true);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb reclassifyArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(RECLASSIFY_ARCHIVE, tenant, user, app, contract, true);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb eliminateArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(ELIMINATE_ARCHIVE, tenant, user, app, contract, true, true);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb exportArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(EXPORT_ARCHIVE, tenant, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb createOrganizationOp(Long tenant, String user, String app) {
    return new OperationDb(CREATE_ORGANIZATION, tenant, user, app, null, true);
  }

  public static OperationDb updateOrganizationOp(Long tenant, String user, String app) {
    return new OperationDb(UPDATE_ORGANIZATION, tenant, user, app, null, true);
  }

  public static OperationDb createUserOp(Long tenant, String user, String app) {
    return new OperationDb(CREATE_USER, tenant, user, app, null, true);
  }

  public static OperationDb updateUserOp(Long tenant, String user, String app) {
    return new OperationDb(UPDATE_USER, tenant, user, app, null, true);
  }

  public static OperationDb createTenantOp(Long tenant, String user, String app) {
    return new OperationDb(CREATE_TENANT, tenant, user, app, null, true);
  }

  public static OperationDb updateTenantOp(Long tenant, String user, String app) {
    return new OperationDb(UPDATE_TENANT, tenant, user, app, null, true);
  }

  public static OperationDb createReferentialOp(
      OperationType opType, Long tenant, String user, String app) {
    OperationDb ope = new OperationDb(opType, tenant, user, app, null, true);
    ope.setStatus(OperationStatus.BACKUP);
    ope.setMessage("Create referential");
    return ope;
  }

  public static OperationDb updateReferentialOp(
      OperationType opType, Long tenant, String user, String app) {
    OperationDb ope = new OperationDb(opType, tenant, user, app, null, true);
    ope.setStatus(OperationStatus.BACKUP);
    ope.setMessage("Update referential");
    return ope;
  }

  public static OperationDb deleteReferentialOp(
      OperationType opType, Long tenant, String user, String app) {
    OperationDb ope = new OperationDb(opType, tenant, user, app, null, true);
    ope.setStatus(OperationStatus.BACKUP);
    ope.setMessage("Delete referential");
    return ope;
  }

  public static OperationDb securingOp(Long tenant) {
    return new OperationDb(TRACEABILITY, tenant, "0", "SYSTEM", null, true);
  }

  public static OperationDb createExternalOp(Long tenant) {
    return new OperationDb(EXTERNAL, tenant, null, null, null, true);
  }
}
