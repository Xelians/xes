/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.Hash;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AtrDetail {

  @NonNull
  @JsonProperty("OperationId")
  private Long operationId;

  @NonNull
  @JsonProperty("Algorithm")
  private Hash algorithm;

  @NonNull
  @JsonProperty("StorageAtrChecksum")
  private String storageAtrChecksum;

  @NonNull
  @JsonProperty("LogbookAtrChecksum")
  private String lbkAtrChecksum;

  @NonNull
  @JsonProperty("Status")
  private ReportStatus status;

  @JsonProperty("StatusDetail")
  private String statusDetail;
}
