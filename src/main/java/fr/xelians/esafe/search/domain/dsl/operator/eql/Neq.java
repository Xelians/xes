/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.NeqOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.field.BooleanField;
import fr.xelians.esafe.search.domain.field.DoubleField;
import fr.xelians.esafe.search.domain.field.IntegerField;
import fr.xelians.esafe.search.domain.field.LongField;
import lombok.ToString;

@ToString
public class Neq extends NeqOperator<Query> {

  // { "$eq": { "societe.facture": "client1" } , "$type": "Facture"  }
  public Neq(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    FieldValue fieldValue =
        switch (field.getType()) {
          case LongField.TYPE, IntegerField.TYPE -> FieldValue.of((Long) value);
          case DoubleField.TYPE -> FieldValue.of((Double) value);
          case BooleanField.TYPE -> FieldValue.of((Boolean) value);
          default -> FieldValue.of(value.toString());
        };

    Query tquery = TermQuery.of(t -> t.field(field.getFullName()).value(fieldValue))._toQuery();
    Query query = BoolQuery.of(b -> b.mustNot(tquery))._toQuery();
    return doctypeQuery(query);
  }
}
