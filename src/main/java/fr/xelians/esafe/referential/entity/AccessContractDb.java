/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.referential.domain.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import lombok.Getter;
import lombok.Setter;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(
    name = "access_contract",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_accesscontract_tenant_identifier",
          columnNames = {"tenant", "identifier"}),
      @UniqueConstraint(
          name = "unique_accesscontract_tenant_name",
          columnNames = {"tenant", "name"})
    })
public class AccessContractDb extends AbstractReferentialDb {

  /** Liste les RootUnits autorisées */
  @NotNull private HashSet<Long> rootUnits = new HashSet<>();

  /** Liste les RootUnits interdites */
  @NotNull private HashSet<Long> excludedRootUnits = new HashSet<>();

  /** Liste les DataObjectVersion (BinaryMaster, dissemination, thumbnail, etc.) autorisées */
  @NotNull private HashSet<String> dataObjectVersion = new HashSet<>();

  /**
   * Autorise l'accès à toutes les DataObjectVersion (BinaryMaster, dissemination, thumbnail, etc.)
   */
  @NotNull private Boolean everyDataObjectVersion = DefaultValue.EVERY_DATA_OBJECT_VERSION;

  /** Liste les Originating Agency autorisées */
  @NotNull private HashSet<String> originatingAgencies = new HashSet<>();

  /** Autorise l'accès à toutes les Originating Agency */
  @NotNull private Boolean everyOriginatingAgency = DefaultValue.EVERY_ORIGINATING_AGENCY;

  /** Autorise la mise à jour (Update) des métadonnées d'une archive unit */
  @NotNull private Boolean writingPermission = DefaultValue.WRITING_PERMISSION;

  /** Interdit la mise à jour les champs de management d'une archive unit */
  @NotNull private Boolean writingRestrictedDesc = DefaultValue.WRITING_RESTRICTED_DESC;

  /** Autorise l'accès aux access logs */
  @NotNull private Status accessLog = DefaultValue.ACCESS_LOG;

  // TODO
  //  private HashSet<RuleType> ruleCategoryToFilter;

}

/*
Hibernate automatically translates the bean validation annotations applied to the entities into the DDL schema metadata.
For example @NotNull is translated to @Column(nullable = false). This feature is enabled when the property
hibernate.validator.apply_to_ddl property is set to true (which is normally the default).
*/
