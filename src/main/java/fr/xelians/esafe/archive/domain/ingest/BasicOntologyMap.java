/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * @author Emmanuel Deviller
 */
public class BasicOntologyMap implements OntologyMap {

  private final Map<String, String> mappings;

  public BasicOntologyMap(List<Mapping> mappings) {
    this.mappings = mappings.stream().collect(Collectors.toMap(Mapping::src, Mapping::dst));
  }

  public BasicOntologyMap(Map<String, String> mappings) {
    this.mappings = mappings;
  }

  @Override
  public String get(String src) {
    return mappings.get(src);
  }

  @Override
  public boolean containsKey(String src) {
    return mappings.containsKey(src);
  }
}
