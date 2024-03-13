/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.parser.eql;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

public record AggCount(Aggregation aggregation, int count) {}
