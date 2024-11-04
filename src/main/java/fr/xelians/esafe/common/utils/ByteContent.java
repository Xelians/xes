/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import java.util.Arrays;

/*
 * @author Emmanuel Deviller
 */
public record ByteContent(String name, byte[] bytes) {

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ByteContent that = (ByteContent) o;
    return name.equals(that.name) && Arrays.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Arrays.hashCode(bytes);
    return result;
  }

  @Override
  public String toString() {
    return "ByteContent{" + "name='" + name + '\'' + ", bytes=" + Arrays.toString(bytes) + '}';
  }
}
