/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.ingest.Mapping;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OntologyDto extends AbstractReferentialDto {

  @NotEmpty
  @Size(max = 5000)
  @JsonProperty("Mappings")
  private List<Mapping> mappings;
}
