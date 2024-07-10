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

  public static <T> List<T> concatList(T value, List<T> list) {
    List<T> concatList = new ArrayList<>(list.size() + 1);
    concatList.add(value);
    concatList.addAll(list);
    return concatList;
  }

  public static <T> List<T> sortedList(List<T> list, Comparator<T> comparator) {
    List<T> sortedList = new ArrayList<>(list);
    sortedList.sort(comparator);
    return sortedList;
  }

  public static <T> List<T> shuffleList(List<T> list) {
    List<T> shuffleList = new ArrayList<>(list);
    Collections.shuffle(shuffleList);
    return shuffleList;
  }

  /**
   * Returns the first item in the given list, or null if not found.
   *
   * @param <T> The generic list type.
   * @param list The list that may have a first item.
   * @return null if the list is null or there is no first item.
   */
  public static <T> T getFirst(final List<T> list) {
    return getFirst(list, null);
  }

  /**
   * Returns the last item in the given list, or null if not found.
   *
   * @param <T> The generic list type.
   * @param list The list that may have a last item.
   * @return null if the list is null or there is no last item.
   */
  public static <T> T getLast(final List<T> list) {
    return getLast(list, null);
  }

  /**
   * Returns the first item in the given list, or t if not found.
   *
   * @param <T> The generic list type.
   * @param list The list that may have a first item.
   * @param t The default return value.
   * @return null if the list is null or there is no first item.
   */
  public static <T> T getFirst(final List<T> list, final T t) {
    return isEmpty(list) ? t : list.get(0);
  }

  /**
   * Returns the last item in the given list, or t if not found.
   *
   * @param <T> The generic list type.
   * @param list The list that may have a last item.
   * @param t The default return value.
   * @return null if the list is null or there is no last item.
   */
  public static <T> T getLast(final List<T> list, final T t) {
    return isEmpty(list) ? t : list.get(list.size() - 1);
  }

  /**
   * Returns true if the given list is null or empty.
   *
   * @param <T> The generic list type.
   * @param list The list that has a last item.
   * @return true The list is empty.
   */
  public static <T> boolean isEmpty(final List<T> list) {
    return list == null || list.isEmpty();
  }

  public static <T> Stream<List<T>> chunk(Stream<T> stream, int size) {
    Iterable<List<T>> litsIterable = () -> ListIterator.iterator(stream.iterator(), size);
    return StreamSupport.stream(litsIterable.spliterator(), false);
  }
}
