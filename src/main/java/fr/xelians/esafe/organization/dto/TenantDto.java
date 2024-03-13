/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.common.dto.AbstractBaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantDto extends AbstractBaseDto {

  @JsonProperty("Tenant")
  private Long id;

  @NotBlank
  @RegularChar
  @JsonProperty("StorageOffers")
  @Size(max = 5)
  private ArrayList<String> storageOffers = new ArrayList<>();

  @JsonProperty("IsEncrypted")
  private Boolean encrypted = DefaultValue.ENCRYPTED;

  // @JsonProperty(value = "OrganizationIdentifier", access = JsonProperty.Access.READ_ONLY)
  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 64)
  @JsonProperty("OrganizationIdentifier")
  private String organizationIdentifier;
}
