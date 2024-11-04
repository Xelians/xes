/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import java.util.List;
import org.springframework.data.domain.Page;

/*
 * @author Emmanuel Deviller
 */
public record PageResult<T>(
    @JsonProperty("$hits") Hits hits, @JsonProperty("$results") List<T> results) {

  public PageResult(Page<T> page) {
    this(
        new Hits(
            (long) page.getNumber() * (long) page.getSize(),
            page.getSize(),
            (long) page.getNumber() * (long) page.getSize() + page.getNumberOfElements(),
            page.getTotalElements()),
        page.getContent());
  }
}
