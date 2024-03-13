/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ListIterator<T> implements Iterator<List<T>> {

  private final Iterator<T> iterator;
  private final int size;

  ListIterator(final Iterator<T> iterator, final int size) {
    this.iterator = iterator;
    this.size = size;
  }

  public static <T> Iterator<List<T>> iterator(Iterable<T> iterable, int size) {
    return new ListIterator<>(iterable.iterator(), size);
  }

  public static <T> Iterator<List<T>> iterator(Iterator<T> iterator, int size) {
    return new ListIterator<>(iterator, size);
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public List<T> next() {
    if (iterator.hasNext()) {
      return createList();
    }
    throw new NoSuchElementException("Element not found");
  }

  private List<T> createList() {
    List<T> list = new ArrayList<>(size);
    while (iterator.hasNext() && list.size() < size) {
      list.add(iterator.next());
    }
    return list;
  }
}
