/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.entity;

import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_organization_identifier",
          columnNames = {"identifier"})
    })
@Entity
public class OrganizationDb extends AbstractBaseDb {

  @Id
  @GeneratedValue(generator = "global_generator")
  protected Long id;

  @Column(nullable = false, updatable = false)
  @NotNull
  @Length(min = 1, max = 64)
  protected String identifier;

  // This is the default tenant for this organization.
  // The default tenant cannot be modified or removed
  @Min(value = 0)
  protected Long tenant;
}
