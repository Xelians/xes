/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import com.google.json.JsonSanitizer;
import fr.xelians.esafe.archive.domain.ingest.Mapping;
import io.jsonwebtoken.lang.Assert;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.BeanUtils;

/**
 * @author Emmanuel Deviller
 */
public final class Utils {

  public static final int CPUS = Runtime.getRuntime().availableProcessors();

  public static final String NOT_NULL = "the %s argument must be not null";
  public static final int PAD_SIZE = 6;
  public static final char PAD_CHAR = '0';

  public static final PolicyFactory NO_HTML = new HtmlPolicyBuilder().allowTextIn("'").toFactory();

  private Utils() {}

  // Return true if b == Boolean.TRUE
  public static boolean isTrue(Boolean b) {
    return Boolean.TRUE.equals(b);
  }

  // Return true if b == Boolean.FALSE or b == null
  public static boolean isFalse(Boolean b) {
    return !isTrue(b);
  }

  public static void doNothing() {}

  public static String limitString(String str, int len) {
    return str == null ? "" : str.substring(0, Math.min(len, str.length()));
  }

  public static boolean containsNotAllowedChar(CharSequence cs) {
    return StringUtils.containsAny(cs, ";", "<", ">", "\r", "\n");
  }

  public static boolean isPositiveInteger(String str) {
    if (str == null) {
      return false;
    }
    int length = str.length();
    if (length == 0) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      char c = str.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  public static byte[] longToBytes(long l) {
    byte[] result = new byte[Long.BYTES];
    for (int i = Long.BYTES - 1; i >= 0; i--) {
      result[i] = (byte) (l & 0xFF);
      l >>= Byte.SIZE;
    }
    return result;
  }

  public static long bytesToLong(final byte[] b) {
    long result = 0;
    for (int i = 0; i < Long.BYTES; i++) {
      result <<= Byte.SIZE;
      result |= (b[i] & 0xFF);
    }
    return result;
  }

  public static long clamp(long min, long value, long max) {
    return Math.max(min, Math.min(max, value));
  }

  public static String buildStacktrace(final Exception exception) {
    final StringBuilder stackTrace = new StringBuilder();
    if (exception.getStackTrace() != null) {
      for (final StackTraceElement element : exception.getStackTrace()) {
        stackTrace.append("\t at ").append(element.toString()).append("\n");
      }
    }
    return stackTrace.toString();
  }

  public static String padKey(int i) {
    return org.apache.commons.lang.StringUtils.leftPad(String.valueOf(i), 3, '0');
  }

  public static String padIdentifier(String prefix, long num) {
    return prefix + StringUtils.leftPad(String.valueOf(num), PAD_SIZE, PAD_CHAR);
  }

  public static boolean isNotHtmlSafe(String text) {
    return (StringUtils.containsAny(text, "<", ">"));
  }

  public static String safeJson(String text) {
    return JsonSanitizer.sanitize(text);
  }

  public static boolean isNotHtmlSafe(PolicyFactory policyFactory, String text) {
    return policyFactory.sanitize(text).equals(text);
  }

  public static boolean isNotJsonSafe(String text) {
    return StringUtils.containsAny(text, "{", "}", "[", "]");
  }

  public static boolean isHtmlSafe(PolicyFactory policyFactory, Set<Mapping> mappings) {
    for (Mapping m : mappings) {
      if (!policyFactory.sanitize(m.src()).equals(m.src())) {
        return false;
      }
      if (!policyFactory.sanitize(m.dst()).equals(m.dst())) {
        return false;
      }
    }
    return true;
  }

  public static boolean isHtmlSafe(PolicyFactory policyFactory, List<Mapping> mappings) {
    for (Mapping m : mappings) {
      if (!policyFactory.sanitize(m.src()).equals(m.src())) {
        return false;
      }
      if (!policyFactory.sanitize(m.dst()).equals(m.dst())) {
        return false;
      }
    }
    return true;
  }

  public static boolean isHtmlSafe(PolicyFactory policyFactory, Map<String, String> map) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (!policyFactory.sanitize(entry.getKey()).equals(entry.getKey())) {
        return false;
      }
      if (!policyFactory.sanitize(entry.getValue()).equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  public static void sleep(long t) {
    try {
      Thread.sleep(t);
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  /**
   * @param <T>
   * @param <E>
   * @param source
   * @param target
   * @return
   */
  public static <T, E> E copyProperties(final T source, final E target) {
    if (source != null && target != null) {
      BeanUtils.copyProperties(source, target);
    }
    return target;
  }

  public static boolean startsWith(String str, char... cs) {
    Assert.notNull(str, "Str must be not null");

    if (!str.isEmpty()) {
      char firstChar = str.charAt(0);
      for (char c : cs) {
        if (c == firstChar) return true;
      }
    }
    return false;
  }

  public static void printStackTrace() {
    System.err.println("Printing stack trace:");
    StackTraceElement[] elements = Thread.currentThread().getStackTrace();
    for (int i = 1; i < elements.length; i++) {
      StackTraceElement s = elements[i];
      System.err.println(
          "\tat "
              + s.getClassName()
              + "."
              + s.getMethodName()
              + "("
              + s.getFileName()
              + ":"
              + s.getLineNumber()
              + ")");
    }
  }
}
