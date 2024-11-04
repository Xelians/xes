/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(
    name = "rule",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_rule_tenant_identifier",
          columnNames = {"tenant", "identifier"})
    })
public class RuleDb extends AbstractReferentialDb {

  @NotNull private RuleType type;

  @NotNull
  @Length(min = 1, max = 10)
  private String duration;

  @NotNull private RuleMeasurement measurement;
}
