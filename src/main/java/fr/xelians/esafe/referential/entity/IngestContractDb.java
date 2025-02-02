/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import static fr.xelians.esafe.common.constant.DefaultValue.*;

import fr.xelians.esafe.referential.domain.CheckParentLinkStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
    name = "ingest_contract",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_ingestcontract_tenant_identifier",
          columnNames = {"tenant", "identifier"}),
      @UniqueConstraint(
          name = "unique_ingestcontract_tenant_name",
          columnNames = {"tenant", "name"})
    })
public class IngestContractDb extends AbstractReferentialDb {

  /** Attachment du Contrat d'ingestion */
  @Min(value = 0)
  private Long linkParentId;

  /**
   * Liste des archives units de rattachement autorisées lors d'une opération d'update (ie. de
   * rattachement) spécifiée dans le manifest du SIP
   */
  @NotNull private HashSet<Long> checkParentIds = new HashSet<>();

  /** Autorisation des attachments dans le manifest (Update Operation) */
  @NotNull private CheckParentLinkStatus checkParentLink = CHECK_PARENT_LINK_STATUS;

  @NotNull private HashSet<String> archiveProfiles = new HashSet<>();

  @NotNull private Boolean masterMandatory = MASTER_MANDATORY;

  @NotNull private Boolean everyDataObjectVersion = EVERY_DATA_OBJECT_VERSION;

  @NotNull private HashSet<String> dataObjectVersion = new HashSet<>();

  @NotNull private Boolean formatUnidentifiedAuthorized = FORMAT_UNIDENTIFIED_AUTHORIZED;

  @NotNull private Boolean everyFormatType = EVERY_FORMAT_TYPE;

  @NotNull private HashSet<String> formatType = new HashSet<>();

  @NotNull private Boolean computeInheritedRulesAtIngest = COMPUTE_INHERITED_RULES_AT_INGEST;

  @NotNull private Boolean storeManifest = STORE_MANIFEST;

  private String managementContractId;
}
