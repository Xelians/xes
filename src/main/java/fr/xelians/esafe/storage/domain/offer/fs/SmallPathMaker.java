/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import fr.xelians.esafe.storage.domain.StorageObjectType;

public class SmallPathMaker implements PathMaker {

  public static final SmallPathMaker INSTANCE = new SmallPathMaker();

  private SmallPathMaker() {}

  @Override
  public String makePath(StorageObjectType type, long id) {
    String str = "0" + id;
    int len = str.length();
    return new StringBuilder()
        .append(str.charAt(len - 1))
        .append(str.charAt(len - 2))
        .append("/")
        .append(id)
        .append(".")
        .append(type)
        .toString();
  }
}
