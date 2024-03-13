/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.dto;

import fr.xelians.esafe.common.constraint.NoHtml;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RefreshDto {

  @NotBlank
  @NoHtml
  @Length(min = 1, max = 16384)
  private String accessToken;

  @NotBlank
  @NoHtml
  @Length(min = 1, max = 256)
  private String refreshToken;

  public RefreshDto() {}

  public RefreshDto(String accessToken, String refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }
}
