/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.common.domain.SortDir;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/*
 * @author Emmanuel Deviller
 */
public final class SearchUtils {

  private SearchUtils() {}

  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final int MIN_PAGE_SIZE = 1;
  public static final int MAX_PAGE_SIZE = 1000;

  public static PageRequest createPageRequest(
      int offset, int limit, String sortBy, SortDir sortDir) {
    int pageNumber = Math.max(0, offset);
    int pageSize = Math.clamp(limit, MIN_PAGE_SIZE, MAX_PAGE_SIZE);
    Sort.Direction sd = sortDir == SortDir.asc ? Sort.Direction.ASC : Sort.Direction.DESC;
    return PageRequest.of(pageNumber, pageSize, Sort.by(sd, sortBy));
  }

  public static PageRequest createPageRequest(int offset, int limit) {
    int pageNumber = Math.max(0, offset);
    int pageSize = Math.clamp(limit, MIN_PAGE_SIZE, MAX_PAGE_SIZE);
    return PageRequest.of(pageNumber, pageSize);
  }

  public static int from(int from) {
    return Math.max(from, 0);
  }

  public static int size(int size) {
    return size < MIN_PAGE_SIZE ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
  }
}
