/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

/*
 * @author Emmanuel Deviller
 */
public final class NumUtils {
  private NumUtils() {}

  public static Integer toInt(Object value) {
    if (value instanceof Number v) return v.intValue();
    else if (value instanceof String v) return Integer.valueOf(v);
    throw new NumberFormatException(String.format("Cannot transform '%s' to Integer", value));
  }

  public static Long toLong(Object value) {
    if (value instanceof Number v) return v.longValue();
    else if (value instanceof String v) return Long.valueOf(v);
    throw new NumberFormatException(String.format("Cannot transform '%s' to Long", value));
  }

  public static Double toDouble(Object value) {
    if (value instanceof Number v) return v.doubleValue();
    else if (value instanceof String v) return Double.valueOf(v);
    throw new NumberFormatException(String.format("Cannot transform '%s' to Double", value));
  }

  public static long toLong(Integer value, long defaultValue) {
    return value == null ? defaultValue : value.longValue();
  }

  public static long toLong(Long value, long defaultValue) {
    return value == null ? defaultValue : value;
  }

  public static int toInt(Integer value, long defaultValue) {
    return value == null ? (int) defaultValue : value;
  }

  public static int toInt(Integer value, int defaultValue) {
    return value == null ? defaultValue : value;
  }
}
