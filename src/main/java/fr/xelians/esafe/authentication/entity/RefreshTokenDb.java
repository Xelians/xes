/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.entity;

import fr.xelians.esafe.organization.entity.UserDb;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(indexes = {@Index(columnList = "token")})
@Entity
public class RefreshTokenDb {

  @Id
  @GeneratedValue(generator = "global_generator")
  private long id;

  @ManyToOne
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @NotNull
  private UserDb user;

  @Column(nullable = false, unique = true)
  @NotBlank
  private String token;

  @NotNull private Instant expiryDate;
}
