/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.repository;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.referential.domain.search.Request;
import java.util.List;

public interface CustomReferentialRepository {

  <T> SearchResult<T> search(Request<T> request, SearchQuery query);

  SearchResult<JsonNode> search(
      Request<Object[]> request, SearchQuery query, List<String> projections);
}
