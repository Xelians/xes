/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.domain;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.ChecksumStorageObject;
import fr.xelians.esafe.storage.domain.object.HashStorageObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StorageAction extends HashStorageObject {

  private final ActionType actionType;

  private StorageAction(
      ActionType actionType, Long id, StorageObjectType type, Hash hash, byte[] checksum) {
    super(id, type, hash, checksum);
    this.actionType = actionType;
  }

  public static StorageAction create(String action) {
    String[] tokens = StringUtils.split(action, ';');
    return StorageAction.create(tokens);
  }

  public static StorageAction create(String[] tokens) {
    if (tokens.length == 3) {
      return new StorageAction(
          ActionType.valueOf(tokens[0]),
          Long.parseLong(tokens[1]),
          StorageObjectType.valueOf(tokens[2]),
          null,
          null);
    }
    if (tokens.length >= 5) {
      return new StorageAction(
          ActionType.valueOf(tokens[0]),
          Long.parseLong(tokens[1]),
          StorageObjectType.valueOf(tokens[2]),
          Hash.valueOf(tokens[3]),
          HashUtils.decodeHex(tokens[4]));
    }
    throw new InternalException(
        "Action creation failed",
        String.format("Failed to create action from tokens [%s]", String.join(";", tokens)));
  }

  public static StorageAction create(ActionType actionType, ChecksumStorageObject storageObject) {
    Validate.notNull(storageObject, "storageObject");
    return new StorageAction(
        actionType,
        storageObject.getId(),
        storageObject.getType(),
        storageObject.getHash(),
        storageObject.getChecksum());
  }

  public static StorageAction create(
      ActionType actionType, Long id, StorageObjectType type, Hash hash, byte[] checksum) {
    Validate.notNull(id, "id");
    Validate.notNull(actionType, "actionType");
    Validate.notNull(type, "type");
    Validate.notNull(hash, "hash");
    Validate.notNull(checksum, "checksum");

    return new StorageAction(actionType, id, type, hash, checksum);
  }

  //    public static Action create(ActionType actionType, Long id, StorageObjectType type) {
  //        Validate.notNull(actionType);
  //        Validate.notNull(id);
  //        Validate.notNull(type);
  //
  //        return new Action(actionType, id, type, null, null);
  //    }

}
