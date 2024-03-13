/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
