/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.Hash;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BinaryObjectDetail {

  @NonNull
  @JsonProperty("BinaryObjectId")
  private Long binaryObjectId;

  @NonNull
  @JsonProperty("DataObjectVersion")
  private String dataObjectVersion;

  @NonNull
  @JsonProperty("BinaryObjecSize")
  private Long binaryObjectSize;

  @NonNull
  @JsonProperty("OperationId")
  private Long operationId;

  @NonNull
  @JsonProperty("Algorithm")
  private Hash algorithm;

  @NonNull
  @JsonProperty("UnitBinaryObjectChecksum")
  private String unitBinaryObjectChecksum;

  @NonNull
  @JsonProperty("StorageBinaryObjectChecksum")
  private String storageBinaryObjectChecksum;

  @NonNull
  @JsonProperty("AtrBinaryObjectChecksum")
  private String atrBinaryObjectChecksum;

  @NonNull
  @JsonProperty("GrantDate")
  private LocalDateTime grantDate;

  @NonNull
  @JsonProperty("Status")
  private ReportStatus status;

  @JsonProperty("StatusDetail")
  private String statusDetail;
}
