/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/*
 * @author Emmanuel Deviller
 */
@Component
@Slf4j
public class ObservationUtils implements ApplicationContextAware {

  private static final MutableObject<ObservationRegistry> OBSERVATION_REGISTRY =
      new MutableObject<>();

  private static final MutableObject<Tracer> TRACER = new MutableObject<>();

  public static void publishException(final Exception e) {
    publishException(OBSERVATION_REGISTRY.getValue().getCurrentObservation(), e);
  }

  public static void publishException(final @Nullable Observation observation, final Exception e) {
    Optional.ofNullable(observation)
        .ifPresentOrElse(
            obs -> obs.error(e),
            () ->
                log.warn("Can't publish exception because observation is not the current thread"));
  }

  public static void publishEvent(@Nullable Observation observation, final Event event) {
    Optional.ofNullable(observation)
        .ifPresentOrElse(
            obs -> obs.event(event),
            () ->
                log.warn(
                    "Can't publish events ({}) because observation is not the current thread",
                    event));
  }

  public static ContextSnapshot getObservationCtxSnapshot() {
    ContextRegistry registry = new ContextRegistry();
    registry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());
    return ContextSnapshot.captureAll(registry);
  }

  public static void observe(final String name, final Runnable runnable) {
    Observation observation = Observation.createNotStarted(name, OBSERVATION_REGISTRY.getValue());
    observation.observe(runnable);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    OBSERVATION_REGISTRY.setValue(applicationContext.getBean(ObservationRegistry.class));
    TRACER.setValue(applicationContext.getBean(Tracer.class));
  }

  public static String getTraceId() {
    Span currentSpan = TRACER.getValue().currentSpan();
    if (currentSpan != null) {
      return currentSpan.context().traceId();
    } else {
      return StringUtils.EMPTY;
    }
  }
}
