/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
