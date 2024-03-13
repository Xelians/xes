/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SearchResponseDto extends AbstractVitamUIResponseDto<DescriptiveMetadataDto> {

  @JsonProperty("$context")
  private VitamSearchRequestDto context;

  @JsonProperty("$facetResults")
  private List<FacetResultsDto> facetResults = new ArrayList<>();
}
