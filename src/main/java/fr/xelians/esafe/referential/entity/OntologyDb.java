/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "ontology",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_ontology_tenant_identifier",
          columnNames = {"tenant", "identifier"}),
      @UniqueConstraint(
          name = "unique_ontology_tenant_name",
          columnNames = {"tenant", "name"})
    })
public class OntologyDb extends AbstractReferentialDb {

  // JPA has different behaviour depending on SpringBoot & Hibernate/JPA version
  //    @ElementCollection Map<String, String> (work with SpringBoot 3)
  //    HashMap<String, String> (work with SpringBoot 2.7 but not with SpringBoot 3)
  // Otherwise, we can get error like
  //    Could not determine recommended JdbcType for HashMap<String, String>
  //    Failed to set HashMap to hibernate PersistentMap
  @NotEmpty
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "ontology_mapping", joinColumns = @JoinColumn(name = "ontology_id"))
  @MapKeyColumn(name = "mapping_key")
  @Column(name = "mapping_value")
  private Map<String, String> mappings;
}
