/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.referential.domain.ProfileFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_profile_tenant_identifier",
          columnNames = {"tenant", "identifier"}),
      @UniqueConstraint(
          name = "unique_profile_tenant_name",
          columnNames = {"tenant", "name"})
    })
@Entity
public class ProfileDb extends AbstractReferentialDb {

  private ProfileFormat format;

  @Column(length = 262144)
  private byte[] data;
}
