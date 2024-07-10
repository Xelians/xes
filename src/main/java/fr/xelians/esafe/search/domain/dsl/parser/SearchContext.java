/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.parser;

import lombok.Getter;

@Getter
public class SearchContext {

  private final String docType;

  public SearchContext() {
    this.docType = null;
  }

  public SearchContext(String docType) {
    this.docType = docType;
  }
}
