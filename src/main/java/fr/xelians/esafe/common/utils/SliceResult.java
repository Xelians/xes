/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Slice;

public record SliceResult<T>(
    @JsonProperty("$hits") SliceHits hits, @JsonProperty("$results") List<T> results) {

  public SliceResult(Slice<T> slice) {
    this(
        new SliceHits(
            (long) slice.getNumber() * (long) slice.getSize(),
            slice.getSize(),
            (long) slice.getNumber() * (long) slice.getSize() + slice.getNumberOfElements(),
            slice.hasNext()),
        slice.getContent());
  }

  public record SliceHits(
      @JsonProperty("offset") long offset,
      @JsonProperty("limit") int limit,
      @JsonProperty("size") long size,
      @JsonProperty("next") boolean next) {}
}
