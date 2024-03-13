/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.constant;

import fr.xelians.esafe.referential.domain.CheckParentLinkStatus;
import fr.xelians.esafe.referential.domain.Status;
import java.time.LocalDate;

public final class DefaultValue {

  private DefaultValue() {}

  public static final Boolean EVERY_FORMAT_TYPE = Boolean.TRUE;

  public static final Boolean COMPUTE_INHERITED_RULES_AT_INGEST = Boolean.FALSE;

  public static final Boolean STORE_MANIFEST = Boolean.TRUE;

  public static final Status BASE_STATUS = Status.ACTIVE;

  public static final Boolean ENCRYPTED = Boolean.FALSE;

  public static final Boolean EVERY_ORIGINATING_AGENCY = Boolean.TRUE;

  public static final Boolean WRITING_PERMISSION = Boolean.FALSE;

  public static final Boolean WRITING_RESTRICTED_DESC = Boolean.FALSE;

  public static final Status ACCESS_LOG = Status.INACTIVE;

  public static final CheckParentLinkStatus CHECK_PARENT_LINK_STATUS =
      CheckParentLinkStatus.AUTHORIZED;

  public static final Boolean MASTER_MANDATORY = Boolean.TRUE;

  public static final Boolean EVERY_DATA_OBJECT_VERSION = Boolean.TRUE;

  public static final Boolean FORMAT_UNIDENTIFIED_AUTHORIZED = Boolean.FALSE;

  public static final int AUTO_VERSION = 1;

  public static final LocalDate ACTIVATION_DATE = LocalDate.ofYearDay(1970, 1);

  public static final LocalDate DEACTIVATION_DATE = LocalDate.ofYearDay(3070, 1);

  public static LocalDate creationDate() {
    return LocalDate.now();
  }

  public static LocalDate lastUpdate() {
    return LocalDate.now();
  }
}
