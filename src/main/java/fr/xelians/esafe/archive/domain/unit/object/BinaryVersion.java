/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

/*
 * @author Emmanuel Deviller
 */
public record BinaryVersion(BinaryQualifier qualifier, Integer version) {
  @Override
  public String toString() {
    return version == null ? qualifier.toString() : qualifier + "_" + version;
  }
}
