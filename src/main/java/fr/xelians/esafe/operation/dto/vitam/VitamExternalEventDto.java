/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto.vitam;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.RegularChar;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class VitamExternalEventDto {

  // Not used
  @Length(max = 256)
  @RegularChar
  @JsonProperty("eventIdentifier")
  private String eventIdentifier;

  // Must start with EXT_
  @Length(max = 256)
  @Pattern(regexp = "^EXT_.*$", message = "ID must start with EXT_")
  @RegularChar
  @NotNull
  @JsonProperty("eventType")
  private String eventType;

  // Map to creationDate or ModifiedDate if child
  @JsonProperty("eventDateTime")
  private LocalDateTime eventDateTime;

  // Not used
  @Length(max = 256)
  @RegularChar
  @JsonProperty("eventIdentifierProcess")
  private String eventIdentifierProcess;

  // Not used
  @Length(max = 256)
  @RegularChar
  @JsonProperty("eventIdentifierRequest")
  private String eventIdentifierRequest;

  // Must be EXTERNAL_LOGBOOK
  @Pattern(regexp = "^EXTERNAL_LOGBOOK$", message = "ID must be equal to EXTERNAL_LOGBOOK")
  @JsonProperty("eventTypeProcess")
  private String eventTypeProcess;

  @RegularChar
  @Length(max = 256)
  @JsonProperty("outcome")
  private String outcome;

  // Map to outcome (if outcome is empty)
  @RegularChar
  @Length(max = 256)
  @JsonProperty("outcomeDetail")
  private String outcomeDetail;

  // Map to outcome (if outcome is empty)
  @RegularChar
  @Length(max = 256)
  @JsonProperty("outcomeDetailMessage")
  private String outcomeDetailMessage;

  // Map to ObjectIdentifier
  @RegularChar
  @Length(max = 256)
  @JsonProperty("objectIdentifier")
  private String objectIdentifier;

  // Map to ObjectInfo
  @RegularChar
  @Length(max = 256)
  @JsonProperty("objectIdentifierRequest")
  private String objectIdentifierRequest;

  // Map to user
  @RegularChar
  @Length(max = 256)
  @JsonProperty("agentIdentifier")
  private String agentIdentifier;

  // Map to application id
  @RegularChar
  @Length(max = 256)
  @JsonProperty("agentIdentifierApplicationSession")
  private String agentIdentifierApplicationSession;

  // Map to application id
  @RegularChar
  @Length(max = 256)
  @JsonProperty("agentIdentifierApplication")
  private String agentIdentifierApplication;

  // Map to ObjectData
  @RegularChar
  @Length(max = 16384)
  @JsonProperty("eventDetailData")
  private String eventDetailData;

  @Size(max = 32)
  @JsonProperty("events")
  private List<VitamExternalEventDto> events = new ArrayList<>();
}
