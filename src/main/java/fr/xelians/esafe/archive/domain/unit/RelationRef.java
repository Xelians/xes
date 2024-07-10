/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import fr.xelians.esafe.common.utils.SipUtils;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

@Getter
public abstract class RelationRef<T> {

  protected final T reference;

  protected RelationRef(T reference) {
    Validate.notNull(reference, SipUtils.NOT_NULL, "reference");
    this.reference = reference;
  }
}
