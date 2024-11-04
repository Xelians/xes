/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.domain;

import lombok.Getter;

/*
 * @author Emmanuel Deviller
 */
@Getter
public enum ActionType {
  CREATE,
  UPDATE,
  DELETE
}
