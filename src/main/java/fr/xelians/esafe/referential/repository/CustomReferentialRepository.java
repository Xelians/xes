/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
