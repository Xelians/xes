/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.entity;

import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(
    name = "operation",
    indexes = {@Index(columnList = "tenant, id")})
@DynamicUpdate
public class OperationDb {

  @Id
  @SequenceGenerator(name = "global_generator", sequenceName = "global_seq")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_generator")
  private Long id;

  private Long lbkId;

  @Min(value = 0)
  @Column(nullable = false, updatable = false)
  @NotNull
  private Long tenant;

  @Column(nullable = false, updatable = false)
  @NotNull
  private String userIdentifier;

  @Column(updatable = false)
  private String applicationId;

  @Column(updatable = false)
  private String contractIdentifier;

  @Column(nullable = false, updatable = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  private OperationType type;

  @NotNull private Boolean toSecure;

  @NotNull private Boolean toRegister;

  @NotNull
  @Enumerated(EnumType.STRING)
  private OperationStatus status = OperationStatus.INIT;

  @NotNull
  @Length(max = 16384)
  @Column(columnDefinition = "TEXT")
  private String message = "";

  @Column(nullable = false, updatable = false)
  @NotNull
  private LocalDateTime created = LocalDateTime.now();

  @NotNull private LocalDateTime modified = LocalDateTime.now();

  private String typeInfo = "";

  private String objectIdentifier = "";

  private String objectInfo = "";

  @Length(max = 16384)
  @Column(columnDefinition = "TEXT")
  private String objectData = "";

  private String outcome = "";

  // Do not limit String size. This is PostgreSQL specific type. We could have used @Lob instead
  @Column(columnDefinition = "TEXT")
  private String property01;

  // Do not limit String size. This is PostgreSQL specific type. We could have used @Lob instead
  @Column(columnDefinition = "TEXT")
  private String property02;

  @Length(max = 16384)
  @Column(columnDefinition = "TEXT")
  private String events;

  @NotNull private ArrayList<String> actions = new ArrayList<>();

  public OperationDb() {}

  public boolean isExclusive() {
    return type.isExclusive();
  }

  public OperationDb(
      OperationType opType,
      Long tenant,
      String userIdentifier,
      String applicationId,
      String contractIdentifier) {

    this(opType, tenant, userIdentifier, applicationId, contractIdentifier, false, false);
  }

  public OperationDb(
      OperationType opType,
      Long tenant,
      String userIdentifier,
      String applicationId,
      String contractIdentifier,
      boolean toSecure) {
    this(opType, tenant, userIdentifier, applicationId, contractIdentifier, toSecure, false);
  }

  public OperationDb(
      OperationType opType,
      Long tenant,
      String userIdentifier,
      String applicationId,
      String contractIdentifier,
      boolean toSecure,
      boolean toRegister) {

    this.type = opType;
    this.tenant = tenant;
    this.toSecure = toSecure;
    this.toRegister = toRegister;
    this.userIdentifier = userIdentifier;
    this.applicationId = Objects.toString(applicationId, "");
    this.contractIdentifier = contractIdentifier;
  }

  public void addAction(StorageAction storageAction) {
    Validate.notNull(storageAction, "action");
    Validate.notNull(storageAction.getActionType(), "actionType");
    Validate.notNull(storageAction.getType(), "storagetype");
    Validate.notNull(storageAction.getId(), "id");

    StringBuilder sb =
        new StringBuilder(storageAction.getActionType().toString())
            .append(";")
            .append(storageAction.getId())
            .append(";")
            .append(storageAction.getType());

    if (storageAction.getHash() != null && storageAction.getChecksum() != null) {
      sb.append(";")
          .append(storageAction.getHash())
          .append(";")
          .append(HashUtils.encodeHex(storageAction.getChecksum()));
    }

    actions.add(sb.toString());
  }

  public void resetActions() {
    actions = new ArrayList<>();
  }

  public LogbookOperation toOperationSe() {
    // Base operation properties
    LogbookOperation logbookOperation = new LogbookOperation();
    logbookOperation.setId(id);
    logbookOperation.setTenant(tenant);
    logbookOperation.setType(type);
    logbookOperation.setMessage(message);
    logbookOperation.setUserIdentifier(userIdentifier);
    logbookOperation.setApplicationId(applicationId);
    logbookOperation.setCreated(created);
    logbookOperation.setModified(modified);

    // For logbook operation (search engine and/or lbk)
    logbookOperation.setTypeInfo(typeInfo);
    logbookOperation.setOutcome(outcome);
    logbookOperation.setObjectIdentifier(objectIdentifier);
    logbookOperation.setObjectInfo(objectInfo);
    logbookOperation.setObjectData(objectData);

    logbookOperation.setStorageActions(actions.stream().map(StorageAction::create).toList());
    return logbookOperation;
  }
}
