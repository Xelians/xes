/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang.Validate;

/*
 * @author Emmanuel Deviller
 */
@Getter
@EqualsAndHashCode
@ToString
public class StorageObjectId {

  private final Long id;
  private final StorageObjectType type;

  public StorageObjectId(Long id, StorageObjectType type) {
    Validate.notNull(id);
    Validate.notNull(type);
    this.id = id;
    this.type = type;
  }
}
