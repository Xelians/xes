/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import lombok.Getter;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

@Getter
public enum Hash {
  MD5(MessageDigestAlgorithms.MD5),
  SHA256(MessageDigestAlgorithms.SHA_256),
  SHA512(MessageDigestAlgorithms.SHA_512);

  public static final Hash[] VALUES = Hash.values();

  private final String algorithm;

  Hash(String algorithm) {
    this.algorithm = algorithm;
  }
}
