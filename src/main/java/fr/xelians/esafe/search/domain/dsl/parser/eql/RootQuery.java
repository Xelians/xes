/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.parser.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/*
 * @author Emmanuel Deviller
 */
public record RootQuery(Query query, int depth) {}
