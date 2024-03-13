/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.domain;

import static fr.xelians.esafe.operation.domain.OperationType.*;

import fr.xelians.esafe.archive.domain.ingest.ContextId;
import fr.xelians.esafe.common.utils.OperationUtils;
import fr.xelians.esafe.operation.entity.OperationDb;

public final class OperationFactory {
  private OperationFactory() {}

  public static OperationDb resetIndexOp(Long tenant, String user, String app) {
    return new OperationDb(RESET_INDEX, tenant, false, user, app, null);
  }

  public static OperationDb resetTenantIndexOp(Long tenant, String user, String app) {
    return new OperationDb(RESET_INDEX_CHUNK, tenant, false, user, app, null);
  }

  public static OperationDb rebuildIndexOp(Long tenant, String user, String app) {
    return new OperationDb(REBUILD_SEARCH_ENGINE, tenant, false, user, app, null);
  }

  public static OperationDb addOfferOp(Long tenant, String user, String app, String dstOffer) {
    OperationDb ope = new OperationDb(ADD_OFFER, tenant, false, user, app, null);
    ope.setProperty01(dstOffer);
    return ope;
  }

  public static OperationDb checkCoherencyOp(
      Long tenant, String user, String app, int delay, int duration) {
    OperationDb ope = new OperationDb(CHECK_COHERENCY, tenant, false, user, app, null);
    ope.setProperty01(String.valueOf(delay));
    ope.setProperty02(String.valueOf(duration));
    return ope;
  }

  public static OperationDb checkTenantCoherencyOp(Long tenant, String user, String app) {
    return new OperationDb(CHECK_TENANT_COHERENCY, tenant, false, user, app, null);
  }

  public static OperationDb ingestArchiveOp(
      Long tenant, ContextId contextId, String user, String app) {
    OperationType opType = OperationUtils.getIngestOperationType(contextId);
    return new OperationDb(opType, tenant, true, user, app, null);
  }

  public static OperationDb updateArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(UPDATE_ARCHIVE, tenant, true, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb updateArchiveRulesOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(UPDATE_ARCHIVE_RULES, tenant, true, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb probativeValueOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(PROBATIVE_VALUE, tenant, true, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb reclassifyArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(RECLASSIFY_ARCHIVE, tenant, true, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb eliminateArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(ELIMINATE_ARCHIVE, tenant, true, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb exportArchiveOp(
      Long tenant, String user, String app, String contract, String query) {
    OperationDb ope = new OperationDb(EXPORT_ARCHIVE, tenant, false, user, app, contract);
    ope.setProperty01(contract);
    ope.setProperty02(query);
    return ope;
  }

  public static OperationDb createOrganizationOp(Long tenant, String user, String app) {
    return new OperationDb(CREATE_ORGANIZATION, tenant, true, user, app, null);
  }

  public static OperationDb updateOrganizationOp(Long tenant, String user, String app) {
    return new OperationDb(UPDATE_ORGANIZATION, tenant, true, user, app, null);
  }

  public static OperationDb createUserOp(Long tenant, String user, String app) {
    return new OperationDb(CREATE_USER, tenant, true, user, app, null);
  }

  public static OperationDb updateUserOp(Long tenant, String user, String app) {
    return new OperationDb(UPDATE_USER, tenant, true, user, app, null);
  }

  public static OperationDb createTenantOp(Long tenant, String user, String app) {
    return new OperationDb(CREATE_TENANT, tenant, true, user, app, null);
  }

  public static OperationDb updateTenantOp(Long tenant, String user, String app) {
    return new OperationDb(UPDATE_TENANT, tenant, true, user, app, null);
  }

  public static OperationDb createReferentialOp(
      OperationType opType, Long tenant, String user, String app) {
    return new OperationDb(opType, tenant, true, user, app, null);
  }

  public static OperationDb updateReferentialOp(
      OperationType opType, Long tenant, String user, String app) {
    return new OperationDb(opType, tenant, true, user, app, null);
  }

  public static OperationDb securingOp(Long tenant) {
    return new OperationDb(SECURING, tenant, true, "0", "SYSTEM", null);
  }

  public static OperationDb createExternalOp(Long tenant) {
    return new OperationDb(EXTERNAL, tenant, true, null, null, null);
  }
}
