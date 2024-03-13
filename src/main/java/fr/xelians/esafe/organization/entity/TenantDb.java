/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@SequenceGenerator(name = "tenant_generator", sequenceName = "tenant", allocationSize = 1)
@Getter
@Setter
@Entity
public class TenantDb extends AbstractBaseDb {

  @Id
  @GeneratedValue(generator = "tenant_generator")
  private Long id;

  @NotNull private ArrayList<String> storageOffers = new ArrayList<>();

  private Boolean encrypted = DefaultValue.ENCRYPTED;

  /** Many tenants could link to the same organization */
  @JsonIgnore
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private OrganizationDb organization;
}
