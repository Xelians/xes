/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import java.util.List;
import org.springframework.data.domain.Page;

public record PageResult<T>(
    @JsonProperty("$hits") Hits hits, @JsonProperty("$results") List<T> results) {

  public PageResult(Page<T> page) {
    this(
        new Hits(
            (long) page.getNumber() * (long) page.getSize(),
            page.getSize(),
            (long) page.getNumber() * (long) page.getSize() + page.getNumberOfElements(),
            page.getTotalElements()),
        page.getContent());
  }
}
