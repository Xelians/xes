/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.repository;

import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.operation.dto.vitam.VitamOperationDto;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/*
 * Performance https://medium.com/geekculture/why-pagination-in-spring-is-not-the-most-efficient-solution-ec4858e5ba1e
 * Stream https://medium.com/predictly-on-tech/spring-data-jpa-batching-using-streams-af456ea611fc
 */

public interface OperationRepository
    extends JpaRepository<OperationDb, Long>,
        CustomOperationRepository,
        JpaSpecificationExecutor<OperationDb> {

  // Dto
  @Query(
      "SELECT new fr.xelians.esafe.operation.dto.OperationDto(CAST(o.id AS string), o.tenant, o.type, o.status, o.message, o.events, o.userIdentifier, o.applicationId, o.created, o.modified) FROM OperationDb o WHERE o.tenant=:tenant AND o.id=:id")
  Optional<OperationDto> getOperationDto(@Param("tenant") Long tenant, @Param("id") Long id);

  @Query(
      "SELECT new fr.xelians.esafe.operation.dto.vitam.VitamOperationDto(CAST(o.id AS string), o.message, o.status, o.outcome, o.toSecure) FROM OperationDb o WHERE o.tenant=:tenant AND o.id=:id")
  Optional<VitamOperationDto> getVitamOperationDto(
      @Param("tenant") Long tenant, @Param("id") Long id);

  @Query(
      "SELECT new fr.xelians.esafe.operation.dto.OperationStatusDto(CAST(o.id AS string), o.status, o.message) FROM OperationDb o WHERE o.tenant=:tenant AND o.id=:id")
  Optional<OperationStatusDto> getOperationStatusDto(
      @Param("tenant") Long tenant, @Param("id") Long id);

  @Query("SELECT o.status FROM OperationDb o WHERE o.id=:id")
  Optional<OperationStatus> getOperationStatusDto(@Param("id") Long id);

  // Entity
  @Query("SELECT o.contractIdentifier FROM OperationDb o  WHERE o.tenant=:tenant AND o.id=:id")
  Optional<String> getContractIdentifier(@Param("tenant") Long tenant, @Param("id") Long id);

  Optional<OperationDb> findOneByTenantAndId(Long tenant, Long id);

  List<OperationDb> findFirst200ByStatusAndModifiedLessThan(
      @Param("status") OperationStatus status, @Param("date") LocalDateTime date);

  List<OperationDb> findFirst2000ByStatusOrderByIdAsc(OperationStatus status1);

  List<OperationDb> findFirst2000ByStatusOrStatusOrderByIdAsc(
      OperationStatus status1, OperationStatus status2);

  List<OperationDb> findByTenantAndTypeAndStatusOrderByIdAsc(
      Long tenant, OperationType type, OperationStatus status);

  @Query(
      "SELECT o FROM OperationDb o WHERE o.tenant=:tenant AND o.status IN :status ORDER BY o.id ASC")
  List<OperationDb> find(
      @Param("tenant") Long tenant,
      @Param("status") List<OperationStatus> status,
      Pageable pageable);

  @Query(
      "SELECT o FROM OperationDb o WHERE o.tenant=:tenant AND o.type IN :types ORDER BY o.id ASC")
  List<OperationDb> find(@Param("tenant") Long tenant, @Param("types") List<OperationType> types);

  @Query(
      "SELECT o FROM OperationDb o WHERE o.tenant=:tenant AND o.type IN :types AND o.status IN :status ORDER BY o.id ASC")
  List<OperationDb> find(
      @Param("tenant") Long tenant,
      @Param("types") List<OperationType> types,
      @Param("status") List<OperationStatus> status);

  @Query(
      "SELECT o.id FROM OperationDb o WHERE o.tenant=:tenant AND o.type IN :types AND o.status IN :status")
  List<Long> findFirst(
      @Param("tenant") Long tenant,
      @Param("types") List<OperationType> types,
      @Param("status") List<OperationStatus> status,
      Pageable pageable);

  @Query(
      "SELECT o.id FROM OperationDb o WHERE o.tenant=:tenant AND o.type IN :types AND o.status IN :status ORDER BY o.id DESC")
  List<Long> findDesc(
      @Param("tenant") Long tenant,
      @Param("types") List<OperationType> types,
      @Param("status") List<OperationStatus> status,
      Pageable pageable);

  @Query(
      "SELECT o FROM OperationDb o WHERE o.tenant=:tenant AND (o.id>:idMin AND o.id<=:idMax) AND o.status=:status AND o.toSecure = true ORDER BY o.id ASC")
  List<OperationDb> findForReindex(
      @Param("tenant") Long tenant,
      @Param("idMin") Long idMin,
      @Param("idMax") Long idMax,
      @Param("status") OperationStatus status,
      Pageable pageable);

  @Query(
      "SELECT o FROM OperationDb o WHERE o.tenant=:tenant AND (o.id>:idMin AND o.id<=:idMax) AND o.type in :types AND o.status IN :status ORDER BY o.id asc")
  List<OperationDb> find(
      @Param("tenant") Long tenant,
      @Param("idMin") Long idMin,
      @Param("idMax") Long idMax,
      @Param("types") List<OperationType> types,
      @Param("status") List<OperationStatus> status,
      Pageable pageable);

  // Securing
  @Query(
      "SELECT o FROM OperationDb o WHERE o.tenant>=:tenant AND o.lbkId>:lbkId AND o.status=:status AND o.created<:date AND o.toSecure=true ORDER BY o.tenant ASC, o.lbkId ASC")
  List<OperationDb> findToSecure(
      @Param("tenant") Long tenant,
      @Param("lbkId") Long lbkId,
      @Param("status") OperationStatus status,
      @Param("date") LocalDateTime date,
      Pageable pageable);

  @Modifying
  @Query("update OperationDb o set o.toSecure=:toSecure WHERE o.id=:id")
  void updateSecured(@Param("id") Long id, @Param("toSecure") boolean toSecure);

  // Accession register
  @Query(
      "SELECT o FROM OperationDb o WHERE o.status=:status AND o.toRegister=true ORDER BY o.tenant ASC, o.lbkId ASC")
  List<OperationDb> findToRegister(@Param("status") OperationStatus status);

  @Modifying
  @Query("update OperationDb o set o.toRegister=:toRegister WHERE o.id=:id")
  void updateRegister(@Param("id") Long id, @Param("toRegister") boolean toRegister);

  // General
  @Modifying
  @Query(
      "UPDATE OperationDb o SET o.status=:newStatus, o.message=:newMessage WHERE o.status=:status")
  void updateStatusAndMessage(
      @Param("status") OperationStatus status,
      @Param("newStatus") OperationStatus newStatus,
      @Param("newMessage") String newMessage);

  @Modifying
  @Query(
      "UPDATE OperationDb o SET o.status=:newStatus, o.message=:newMessage WHERE o.status=:status AND o.modified<:date")
  void updateStatusAndMessage(
      @Param("status") OperationStatus status,
      @Param("date") LocalDateTime date,
      @Param("newStatus") OperationStatus newStatus,
      @Param("newMessage") String newMessage);

  void deleteByStatusAndModifiedLessThan(
      @Param("status") OperationStatus status, @Param("date") LocalDateTime date);

  @Query("SELECT o.id FROM OperationDb o WHERE o.status=:status AND o.modified<:date")
  List<Long> findOperationIds(
      @Param("status") OperationStatus status, @Param("date") LocalDateTime date);

  @Modifying
  @Query(
      "DELETE OperationDb o WHERE o.status=:status AND o.toSecure=false AND o.toRegister=false AND o.modified<:date")
  void deleteSecuredOperation(
      @Param("status") OperationStatus status, @Param("date") LocalDateTime date);

  //  @QueryHints(value = {@QueryHint(name = HINT_FETCH_SIZE, value = "1"),
  //      @QueryHint(name = HINT_CACHEABLE, value = "false"), @QueryHint(name = HINT_READONLY, value
  // = "true"),
  //      @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")})
  //  @Query("select o FROM OperationDb o WHERE o.status=:status order by o.id")
  //  Stream<OperationDb> findAllByStatusAndStream(@Param("status") OperationStatus status);

  //  @Modifying
  //  @Query("update OperationDb o set o.storageOffers=:offers WHERE o.tenant=:tenant")
  //  void updateOffers(@Param("tenant") Long tenant, @Param("offers") List<String> offers);

}
