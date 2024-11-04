/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class Perfs {

  private static final BigDecimal NANO = BigDecimal.valueOf(1_000_000_000);
  private final String description;
  private final AtomicLong timer = new AtomicLong();
  private final AtomicLong elapsed = new AtomicLong();
  private final AtomicLong count = new AtomicLong();

  public Perfs(String description) {
    this.description = description;
  }

  public static Perfs start() {
    return start(null);
  }

  public static Perfs start(String description) {
    if (description != null) {
      log.info(description);
    }

    Perfs p = new Perfs(description);
    p.timer.set(System.nanoTime());
    p.count.set(1);
    return p;
  }

  public void reset() {
    timer.set(System.nanoTime());
    count.set(1);
  }

  public void resume() {
    timer.set(System.nanoTime());
    count.incrementAndGet();
  }

  public void pause() {
    long time = System.nanoTime();
    elapsed.addAndGet(time - timer.get());
  }

  public Perfs log() {
    return log(description);
  }

  public Perfs log(String desc) {
    long diff = System.nanoTime() - timer.get();
    BigDecimal total = BigDecimal.valueOf(diff);
    BigDecimal ops = BigDecimal.valueOf(-1);

    if (diff > 0) {
      total = total.divide(NANO, MathContext.DECIMAL64);
      ops = BigDecimal.valueOf(count.get()).divide(total, 2, RoundingMode.HALF_UP);
      total = total.setScale(3, RoundingMode.HALF_UP);
    }

    if (StringUtils.isBlank(desc)) {
      log.info("Time: {} s - {} - {} op/s", total, count.get(), ops);
    } else {
      log.info("Time {}: {} s - {} - {} op/s", desc, total, count.get(), ops);
    }
    return this;
  }

  public Perfs logElapsed() {
    return logElapsed(description);
  }

  public Perfs logElapsed(String description) {
    double diff = elapsed.get();
    BigDecimal total = BigDecimal.valueOf(diff);
    BigDecimal ops = BigDecimal.valueOf(-1);

    if (diff > 0) {
      total = total.divide(NANO, MathContext.DECIMAL64);
      ops = BigDecimal.valueOf(count.get()).divide(total, 3, RoundingMode.HALF_UP);
      total = total.setScale(2, RoundingMode.HALF_UP);
    }

    if (StringUtils.isBlank(description)) {
      log.info("Elapsed: {} s - {} - {} op/s", total, count.get(), ops);
    } else {
      log.info("Elapsed {}: {} s - {} - {} op/s", description, total, count.get(), ops);
    }
    return this;
  }

  public String getDescription() {
    return description;
  }
}
