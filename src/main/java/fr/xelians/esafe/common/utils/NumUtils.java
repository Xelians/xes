package fr.xelians.esafe.common.utils;

public final class NumUtils {
  private NumUtils() {}

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
