/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.hashset;

import java.util.Iterator;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public interface IdSet extends Iterable<Long>, AutoCloseable {

  void add(long id);

  void remove(long id);

  long size();

  void close();

  Iterator<List<Long>> listIterator();
}
