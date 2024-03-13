/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProbativeValueReport {

  @NonNull
  @JsonProperty("Date")
  private LocalDateTime date;

  @NonNull
  @JsonProperty("Tenant")
  private Long tenant;

  @NonNull
  @JsonProperty("OperationId")
  private Long operationId;

  @NonNull
  @JsonProperty("Type")
  private ReportType type;

  @NonNull
  @JsonProperty("Status")
  ReportStatus status;

  @NonNull
  @JsonProperty("BinaryObjectDetail")
  private List<BinaryObjectDetail> binaryObjectDetails = new ArrayList<>();

  @NonNull
  @JsonProperty("AtrDetail")
  private List<AtrDetail> atrDetails = new ArrayList<>();
}
