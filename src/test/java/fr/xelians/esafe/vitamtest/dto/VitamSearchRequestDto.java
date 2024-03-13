/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.QueryDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VitamSearchRequestDto {

  @JsonProperty("$roots")
  private List<String> roots = new ArrayList<>();

  @JsonProperty("$query")
  private List<QueryDTO> query = new ArrayList<>();

  @JsonProperty("$filter")
  private FilterDto filter;

  @JsonProperty("$projection")
  private ProjectionDto projection;
}
