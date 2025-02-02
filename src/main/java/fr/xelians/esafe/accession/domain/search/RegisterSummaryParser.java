/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.domain.search;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.search.domain.dsl.parser.eql.SimpleEqlParser;

/*
 * @author Emmanuel Deviller
 */
public class RegisterSummaryParser extends SimpleEqlParser {

  private RegisterSummaryParser(Long tenant) {
    super(RegisterSummaryIndex.INSTANCE, tenant);
  }

  public static SearchRequest createRequest(Long tenant, SearchQuery query) {
    RegisterSummaryParser parser = new RegisterSummaryParser(tenant);
    return parser.createSearchRequest(query);
  }
}
