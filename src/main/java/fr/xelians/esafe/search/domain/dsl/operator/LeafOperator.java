/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
public abstract class LeafOperator<T> implements LeafQueryOperator<T> {

  public static final String DOCUMENT_TYPE = "DocumentType";
  public static final String TYPE = "$type";

  protected final DslParser<T> parser;
  protected final SearchContext searchContext;
  protected String docType;

  protected LeafOperator(DslParser<T> parser, SearchContext searchContext) {
    this.parser = parser;
    this.searchContext = searchContext;
    this.docType = searchContext.getDocType();
  }

  protected Query doctypeQuery(Query query) {
    return StringUtils.isBlank(docType)
        ? query
        : BoolQuery.of(
                b ->
                    b.must(query)
                        .filter(
                            TermQuery.of(t -> t.field(DOCUMENT_TYPE).value(docType))._toQuery()))
            ._toQuery();
  }
}
