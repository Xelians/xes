/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import fr.xelians.esafe.storage.domain.StorageObjectType;

public class MediumPathMaker implements PathMaker {

  public static final MediumPathMaker INSTANCE = new MediumPathMaker();

  private MediumPathMaker() {}

  @Override
  public String makePath(StorageObjectType type, long id) {
    String str = "000" + id;
    int len = str.length();
    return ""
        + str.charAt(len - 1)
        + str.charAt(len - 2)
        + "/"
        + str.charAt(len - 3)
        + str.charAt(len - 4)
        + "/"
        + id
        + "."
        + type; // keep the first ""
  }
}
