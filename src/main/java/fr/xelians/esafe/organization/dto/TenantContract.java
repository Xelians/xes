/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.common.exception.technical.InternalException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@EqualsAndHashCode
public class TenantContract {

  @NotNull
  @JsonProperty("Tenant")
  private Long tenant;

  @NoHtml
  @NotBlank
  @RegularChar
  @JsonProperty("Identifier")
  private String identifier;

  @JsonCreator
  public TenantContract(
      @JsonProperty("Tenant") Long tenant, @JsonProperty("Identifier") String identifier) {
    this.tenant = tenant;
    this.identifier = identifier;
  }

  public TenantContract(String contract) {
    String[] tks = StringUtils.split(contract, ":", 2);
    if (tks.length != 2) {
      throw new InternalException(String.format("Bad tenant contract '%s", contract));
    }
    this.tenant = Long.parseLong(tks[0]);
    this.identifier = tks[1];
  }

  public String toString() {
    return tenant + ":" + identifier;
  }
}
