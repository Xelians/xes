/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import java.util.List;
import java.util.concurrent.*;

/*
 * @author Emmanuel Deviller
 */
public class ThreadUtils {

  private ThreadUtils() {}

  // Block the caller if the queue is larger than maxQueueSize
  public static ThreadPoolExecutor blockingPool(int maxPoolSize, int maxQueueSize) {
    // Note. this only works for thread pool where corePoolSize==maxPoolSize
    return new ThreadPoolExecutor(
        maxPoolSize, maxPoolSize, 0, TimeUnit.MILLISECONDS, new LimitedQueue<>(maxQueueSize));
  }

  public static void joinFutures(List<Future<?>> futures, long timeout, TimeUnit timeUnit)
      throws ExecutionException, InterruptedException, TimeoutException {
    for (Future<?> future : futures) {
      future.get(timeout, timeUnit);
    }
  }

  public static <T> void joinFutures(List<Future<T>> futures)
      throws ExecutionException, InterruptedException {
    for (Future<T> future : futures) {
      future.get();
    }
  }

  private static class LimitedQueue<E> extends LinkedBlockingQueue<E> {

    public LimitedQueue(int maxSize) {
      super(maxSize);
    }

    @Override
    public boolean offer(E e) {
      if (e == null) throw new NullPointerException();

      // turn offer() and add() into a blocking calls (unless interrupted)
      try {
        put(e);
        return true;
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      return false;
    }
  }
}
