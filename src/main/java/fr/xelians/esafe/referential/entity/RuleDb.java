/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.referential.domain.RuleMeasurement;
import fr.xelians.esafe.referential.domain.RuleType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_rule_tenant_identifier",
          columnNames = {"tenant", "identifier"})
    })
@Entity
public class RuleDb extends AbstractReferentialDb {

  @NotNull private RuleType type;

  @NotNull
  @Length(min = 1, max = 10)
  private String duration;

  @NotNull private RuleMeasurement measurement;
}
