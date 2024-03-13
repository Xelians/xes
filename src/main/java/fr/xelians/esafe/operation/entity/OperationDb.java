/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.entity;

import static fr.xelians.esafe.sequence.Sequence.ALLOCATION_SIZE;

import fr.xelians.esafe.common.utils.HashUtils;
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

@SequenceGenerator(
    name = "global_generator",
    sequenceName = "global",
    allocationSize = ALLOCATION_SIZE)
@Getter
@Setter
@Table(indexes = {@Index(columnList = "tenant, id")})
@Entity
@DynamicUpdate
public class OperationDb {

  @Id
  @GeneratedValue(generator = "global_generator")
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

  @Column(nullable = false, updatable = false)
  @NotNull
  private Boolean toSecure;

  @NotNull private Boolean secured;

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
      boolean toSecure,
      String userIdentifier,
      String applicationId,
      String contractIdentifier) {

    this.type = opType;
    this.tenant = tenant;
    this.toSecure = toSecure;
    this.secured = false;
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

  public OperationSe toOperationSe() {
    // Base operation properties
    OperationSe operationSe = new OperationSe();
    operationSe.setId(id);
    operationSe.setTenant(tenant);
    operationSe.setType(type);
    operationSe.setMessage(message);
    operationSe.setUserIdentifier(userIdentifier);
    operationSe.setApplicationId(applicationId);
    operationSe.setCreated(created);
    operationSe.setModified(modified);

    // For logbook operation (search engine and/or lbk)
    operationSe.setTypeInfo(typeInfo);
    operationSe.setOutcome(outcome);
    operationSe.setObjectIdentifier(objectIdentifier);
    operationSe.setObjectInfo(objectInfo);
    operationSe.setObjectData(objectData);

    operationSe.setStorageActions(actions.stream().map(StorageAction::create).toList());
    return operationSe;
  }
}
