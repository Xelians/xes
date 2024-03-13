/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.repository;

import fr.xelians.esafe.organization.entity.OrganizationDb;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationDb, Long> {

  Optional<OrganizationDb> findByIdentifier(String identifier);

  Optional<OrganizationDb> findByTenant(Long tenant);

  boolean existsByIdentifier(String identifier);
}
