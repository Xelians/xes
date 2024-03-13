/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.referential.domain.RuleMeasurement;
import fr.xelians.esafe.referential.domain.RuleType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RuleDto extends AbstractReferentialDto {

  @NotNull
  @JsonProperty("Type")
  private RuleType type;

  @NoHtml
  @NotNull
  @RegularChar
  @Length(min = 1, max = 10)
  @JsonProperty("Duration")
  private String duration;

  @NotNull
  @JsonProperty("Measurement")
  private RuleMeasurement measurement;

  // Maintain VITAM compatibility
  @JsonProperty("Value")
  @Override
  public void setName(String name) {
    this.name = name;
  }

  // Maintain VITAM compatibility
  @JsonProperty("Value")
  @Override
  public String getName() {
    return this.name;
  }
}
