/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

  @NotBlank
  @Length(min = 64, max = 2048)
  private String secret;

  @Min(1)
  @Max(3600)
  private long accessTokenExpiration = 900L;

  @Min(60)
  @Max(43200)
  private long refreshTokenExpiration = 36000L;
}
