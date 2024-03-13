/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.probativevalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

public record ProbativeValueQuery(
    @NotNull @JsonProperty("dslQuery") SearchQuery searchQuery,
    @NotNull @JsonProperty("usage") @Size(max = 4) Set<BinaryQualifier> usages,
    @NotNull @JsonProperty("version") String version) {

  public ProbativeValueQuery {
    if (CollectionUtils.isEmpty(usages)) usages = Set.of(BinaryQualifier.BinaryMaster);
    if (StringUtils.isBlank(version)) version = "LAST";
  }
}
