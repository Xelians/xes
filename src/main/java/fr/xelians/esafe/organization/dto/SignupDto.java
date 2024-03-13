/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class SignupDto {

  @NotNull
  @JsonProperty("Organization")
  private OrganizationDto organizationDto;

  @NotNull
  @JsonProperty("User")
  private UserDto userDto;

  @NotNull
  @JsonProperty("Tenant")
  private TenantDto tenantDto;
}
