/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractReferentialDb extends AbstractBaseDb implements ReferentialDb {

  @Id
  @GeneratedValue(generator = "global_generator")
  protected Long id;

  @Column(nullable = false, updatable = false, length = 64)
  @NotNull
  @Length(min = 1, max = 64)
  protected String identifier;

  @NotNull
  @Min(value = 0)
  @Column(nullable = false, updatable = false)
  protected Long tenant;
}
