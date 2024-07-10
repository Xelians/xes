/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.Operator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.EqlParser;

public abstract class Patch implements Operator {

  public static final String CREATION_FAILED = "Failed to create update query with %s operator";

  public static final String DOCUMENT_TYPE = "DocumentType";
  public static final String TYPE = "$type";

  protected final EqlParser parser;
  protected final SearchContext searchContext;
  protected String docType;

  protected Patch(EqlParser parser, SearchContext searchContext) {
    this.parser = parser;
    this.searchContext = searchContext;
    this.docType = searchContext.getDocType();
  }

  public abstract JsonNode getJsonPatchOp();
}
