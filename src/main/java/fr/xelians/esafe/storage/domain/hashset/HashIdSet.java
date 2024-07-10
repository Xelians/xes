/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.hashset;

import fr.xelians.esafe.common.utils.ListIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HashIdSet implements IdSet {

  public static final int LIST_SIZE = 50_000;

  private final Set<Long> ids;

  public HashIdSet(int capacity) {
    ids = HashSet.newHashSet(capacity);
  }

  @Override
  public void add(long id) {
    ids.add(id);
  }

  @Override
  public void remove(long id) {
    ids.remove(id);
  }

  @Override
  public long size() {
    return ids.size();
  }

  @Override
  public Iterator<Long> iterator() {
    return ids.iterator();
  }

  @Override
  public void close() {
    // Do nothing
  }

  @Override
  public Iterator<List<Long>> listIterator() {
    return ListIterator.iterator(this, LIST_SIZE);
  }
}
