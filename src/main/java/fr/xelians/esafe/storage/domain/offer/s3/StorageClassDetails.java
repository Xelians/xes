/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

/*
 * @author Emmanuel Deviller
 */
public record StorageClassDetails(
    String primaryStorageClass,
    String secondaryStorageClass,
    boolean needRestore,
    long secondarySize) {}
