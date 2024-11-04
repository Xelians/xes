/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.repository;

import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.ReferentialDb;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/*
 * @author Emmanuel Deviller
 */
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
      "SELECT identifier FROM #{#entityName} e WHERE e.tenant = ?1 AND e.identifier LIKE CONCAT(?2,'%')")
  List<String> findIdentifiersByTenantAndStartingWith(Long tenant, String start);

  @Query("SELECT id FROM #{#entityName} e WHERE e.tenant = ?1 AND e.identifier = ?2")
  Optional<Long> findIdByTenantAndIdentifier(Long tenant, String identifier);

  @Query("SELECT id FROM #{#entityName} e WHERE e.tenant=:tenant AND e.identifier IN :identifiers")
  List<Long> findIdByTenantAndIdentifiers(
      @Param("tenant") Long tenant, @Param("identifiers") List<String> identifiers);

  boolean existsByTenantAndIdentifier(Long tenant, String identifier);

  @Modifying
  @Query(
      "UPDATE #{#entityName} o SET o.status=:status, o.lastUpdate=:lastUpdate WHERE o.tenant=:tenant AND o.identifier=:identifier")
  void update(
      @Param("tenant") Long tenant,
      @Param("identifier") String identifier,
      @Param("status") Status status,
      @Param("lastUpdate") LocalDate lastUpdate);

  @Modifying
  @Query("DELETE FROM #{#entityName} e WHERE e.tenant = ?1")
  void deleteWithTenant(Long tenant);

  @Modifying
  @Query("DELETE FROM #{#entityName} e WHERE e.tenant = ?1 AND e.identifier = ?2")
  void delete(Long tenant, String identifier);
}
