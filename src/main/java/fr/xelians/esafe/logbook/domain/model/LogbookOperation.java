/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.entity.searchengine.DocumentSe;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@ToString
public class LogbookOperation implements DocumentSe {

  @JsonProperty("_operationId")
  private Long id;

  @JsonProperty("_tenant")
  private Long tenant;

  @JsonProperty("Type")
  private OperationType type;

  @JsonProperty("UserIdentifier")
  private String userIdentifier;

  @JsonProperty("ApplicationId")
  private String applicationId;

  @JsonProperty("Created")
  private LocalDateTime created;

  @JsonProperty("Modified")
  private LocalDateTime modified;

  @JsonProperty("TypeInfo")
  private String typeInfo;

  @JsonProperty("Outcome")
  private String outcome;

  @JsonProperty("Message")
  private String message;

  @JsonProperty("ObjectIdentifier")
  private String objectIdentifier;

  @JsonProperty("ObjectInfo")
  private String objectInfo;

  @JsonProperty("ObjectData")
  private String objectData;

  @JsonProperty("_secureNumber")
  private Long secureNumber;

  @JsonIgnore private List<StorageAction> storageActions = new ArrayList<>();

  public LogbookOperation() {}

  public LogbookOperation(
      Long id,
      Long tenant,
      OperationType type,
      String userIdentifier,
      String applicationId,
      LocalDateTime created,
      LocalDateTime modified,
      String typeInfo,
      String outcome,
      String message,
      String objectIdentifier,
      String objectInfo,
      String objectData) {

    this.id = id;
    this.tenant = tenant;
    this.type = type;
    this.userIdentifier = userIdentifier;
    this.applicationId = applicationId;
    this.created = created;
    this.modified = modified;
    this.typeInfo = typeInfo;
    this.outcome = outcome;
    this.message = message;
    this.objectIdentifier = objectIdentifier;
    this.objectInfo = objectInfo;
    this.objectData = objectData;
  }

  public void addAction(StorageAction storageAction) {
    storageActions.add(storageAction);
  }
}
