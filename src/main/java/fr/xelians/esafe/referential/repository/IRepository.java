/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.repository;

import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.ReferentialDb;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface IRepository<E extends ReferentialDb>
    extends JpaRepository<E, Long>, CustomReferentialRepository {

  List<E> findByTenant(Long tenant);

  Optional<E> findByTenantAndIdentifier(Long tenant, String identifier);

  List<E> findByTenantAndName(Long tenant, String name);

  Page<E> findByTenant(Long tenant, Pageable pageRequest);

  Page<E> findByTenantAndName(Long tenant, String name, Pageable pageRequest);

  Page<E> findByTenantAndNameAndStatus(
      Long tenant, String name, Status status, Pageable pageRequest);

  Page<E> findByTenantAndStatus(Long tenant, Status status, Pageable pageRequest);

  @Query("SELECT identifier FROM #{#entityName} e WHERE e.tenant = ?1")
  List<String> findIdentifiersByTenant(Long tenant);

  @Query(
      "SELECT identifier FROM #{#entityName} e WHERE e.tenant = ?1 and e.identifier LIKE CONCAT(?2,'%')")
  List<String> findIdentifiersByTenantAndStartingWith(Long tenant, String start);

  @Query("SELECT id FROM #{#entityName} e WHERE e.tenant = ?1 and e.identifier = ?2")
  Optional<Long> findIdByTenantAndIdentifier(Long tenant, String identifier);

  @Query("SELECT id FROM #{#entityName} e WHERE e.tenant=:tenant AND e.identifier IN :identifiers")
  List<Long> findIdByTenantAndIdentifiers(
      @Param("tenant") Long tenant, @Param("identifiers") List<String> identifiers);

  boolean existsByTenantAndIdentifier(Long tenant, String identifier);
}
