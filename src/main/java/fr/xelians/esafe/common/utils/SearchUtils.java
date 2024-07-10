/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.common.domain.SortDir;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class SearchUtils {

  private SearchUtils() {}

  public static int size(int size) {
    if (size < 1) {
      return 100;
    }
    return Math.min(size, 1000);
  }

  public static int from(int from) {
    return Math.max(from, 0);
  }

  public static PageRequest createPageRequest(
      int offset, int limit, String sortBy, SortDir sortdir) {
    int pageNumber = Math.max(0, offset);
    int pageSize = Math.max(1, Math.min(1000, limit));
    Sort.Direction sd = sortdir == SortDir.asc ? Sort.Direction.ASC : Sort.Direction.DESC;
    return PageRequest.of(pageNumber, pageSize, Sort.by(sd, sortBy));
  }

  public static PageRequest createPageRequest(int offset, int limit) {
    int pageNumber = Math.max(0, offset);
    int pageSize = Math.max(1, Math.min(1000, limit));
    return PageRequest.of(pageNumber, pageSize);
  }
}
