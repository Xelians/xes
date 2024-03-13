/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.common.dto.AbstractBaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OrganizationDto extends AbstractBaseDto {

  // The identifier is automatically created if it does not exist
  // Beware - it's a bad practice to rely on automatic creation
  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 64)
  @JsonProperty("Identifier")
  protected String identifier;

  @JsonProperty("Tenant")
  protected Long tenant;
}
