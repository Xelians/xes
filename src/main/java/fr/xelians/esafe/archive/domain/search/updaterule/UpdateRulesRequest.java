/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import co.elastic.clients.elasticsearch.core.SearchRequest;

/*
 * @author Emmanuel Deviller
 */
public record UpdateRulesRequest(SearchRequest searchRequest, RuleLists ruleLists) {}
