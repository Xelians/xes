/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public final class CollUtils {

  private CollUtils() {}

  /**
   * Concat two maps into a new immutable map. In case of duplicate keys, this method throws an
   * IllegalStateException.
   *
   * @param map1 the first map
   * @param map2 the second map
   * @param <K> the key
   * @param <V> the value
   * @return the new concatenated immutable map
   */
  public static <K, V> Map<K, V> concatMap(Map<K, V> map1, Map<K, V> map2) {
    return Stream.concat(map1.entrySet().stream(), map2.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static <T> List<T> concat(T value, Collection<T> list) {
    List<T> concatList = new ArrayList<>(list.size() + 1);
    concatList.add(value);
    concatList.addAll(list);
    return concatList;
  }

  public static <T> List<T> sort(Collection<T> list, Comparator<T> comparator) {
    List<T> sortedList = new ArrayList<>(list);
    sortedList.sort(comparator);
    return sortedList;
  }

  public static <T> List<T> shuffle(Collection<T> list) {
    List<T> shuffleList = new ArrayList<>(list);
    Collections.shuffle(shuffleList);
    return shuffleList;
  }

  public static <T> List<T> substract(Collection<T> list1, Collection<T> list2) {
    List<T> diffList = new ArrayList<>(list1);
    diffList.removeAll(list2);
    return diffList;
  }

  public static <T> Stream<List<T>> chunk(Stream<T> stream, int size) {
    Iterable<List<T>> litsIterable = () -> ListIterator.iterator(stream.iterator(), size);
    return StreamSupport.stream(litsIterable.spliterator(), false);
  }
}
