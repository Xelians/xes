/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_ontology_tenant_identifier",
          columnNames = {"tenant", "identifier"}),
      @UniqueConstraint(
          name = "unique_ontology_tenant_name",
          columnNames = {"tenant", "name"})
    })
@Entity
public class OntologyDb extends AbstractReferentialDb {

  // JPA has different behaviour depending on SpringBoot & Hibernate/JPA version
  //    @ElementCollection Map<String, String> (work with SpringBoot 3)
  //    HashMap<String, String> (work with SpringBoot 2.7 but not with SpringBoot 3)
  // Otherwise, we can get error like
  //    Could not determine recommended JdbcType for HashMap<String, String>
  //    Failed to set HashMap to hibernate PersistentMap
  @NotEmpty
  @ElementCollection(fetch = FetchType.EAGER)
  private Map<String, String> mappings;
}
