/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.repository;

import fr.xelians.esafe.organization.entity.OrganizationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<TenantDb, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = AvailableSettings.JAKARTA_LOCK_TIMEOUT, value = "10000"))
  @Query("select t from TenantDb t where t.id=:id")
  Optional<TenantDb> findForUpdate(@Param("id") Long id);

  @Query("select t from TenantDb t left join fetch t.organization where t.id=:id")
  Optional<TenantDb> findOneById(@Param("id") Long id);

  Optional<TenantDb> findById(@Param("id") Long id);

  @Query("select id from TenantDb")
  List<Long> findAllIds();

  @Query("select t.storageOffers from TenantDb t where t.id=:id")
  List<String[]> getStorageOffers(@Param("id") Long id);

  boolean existsByIdAndOrganizationId(Long id, Long organizationId);

  Optional<TenantDb> findByIdAndOrganizationId(Long id, Long organizationId);

  List<TenantDb> findAllByOrganization(OrganizationDb organization);

  // @Query("select t from TenantDb t left join fetch t.organization where t.organization.id
  // =:organizationId")
  List<TenantDb> getByOrganizationId(Long organizationId);
}
