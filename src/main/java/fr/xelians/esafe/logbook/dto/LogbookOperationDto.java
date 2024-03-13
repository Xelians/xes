/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.logbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class LogbookOperationDto {

  @JsonProperty("#id")
  @NotNull
  private Long id;

  @JsonProperty("#tenant")
  @NotNull
  private Long tenant;

  @JsonProperty("Type")
  @NotNull
  private OperationType type;

  @JsonProperty("TypeInfo")
  @NotNull
  private String typeInfo;

  @JsonProperty("UserIdentifier")
  @NotNull
  private String userIdentifier;

  @JsonProperty("ApplicationId")
  private String applicationId;

  @JsonProperty("Created")
  @NotNull
  private LocalDateTime created;

  @JsonProperty("Modified")
  @NotNull
  private LocalDateTime modified;

  @JsonProperty("ObjectIdentifier")
  private String objectIdentifier;

  @JsonProperty("ObjectInfo")
  private String objectInfo;

  @JsonProperty("ObjectData")
  private String objectData;

  @JsonProperty("Outcome")
  private String outcome;

  @JsonProperty("Message")
  private String message;
}
