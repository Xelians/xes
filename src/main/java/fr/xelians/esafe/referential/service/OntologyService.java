/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import static java.util.stream.Collectors.toMap;

import fr.xelians.esafe.archive.domain.ingest.Mapping;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitIndex;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.FieldUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.OntologyDto;
import fr.xelians.esafe.referential.entity.OntologyDb;
import fr.xelians.esafe.referential.repository.OntologyRepository;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Service
public class OntologyService extends AbstractReferentialService<OntologyDto, OntologyDb> {

  public static final String CHECK_ONTOLOGY_FAILED = "Check ontology failed";

  @Autowired
  public OntologyService(
      EntityManager entityManager,
      OntologyRepository repository,
      OperationService operationService,
      TenantService tenantService) {
    super(entityManager, repository, operationService, tenantService);
  }

  @Override
  public OntologyDto toDto(OntologyDb entity) {
    OntologyDto dto = Utils.copyProperties(entity, createDto());
    List<Mapping> mappings = new ArrayList<>(entity.getMappings().size());
    entity.getMappings().forEach((key, value) -> mappings.add(new Mapping(key, value)));
    dto.setMappings(mappings);
    return dto;
  }

  @Override
  public OntologyDb toEntity(OntologyDto dto) {
    OntologyDb entity = super.toEntity(dto);
    entity.setMappings(dto.getMappings().stream().collect(toMap(Mapping::src, Mapping::dst)));
    return entity;
  }

  @Override
  public OntologyDb updateDtoToEntity(OntologyDto dto, OntologyDb entity) {
    OntologyDb trgEntity = super.updateDtoToEntity(dto, entity);
    trgEntity.setMappings(dto.getMappings().stream().collect(toMap(Mapping::src, Mapping::dst)));
    return trgEntity;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<OntologyDto> createOntology(
      OperationDb operation, Long tenant, List<OntologyDto> dtos) {
    dtos.forEach(this::checkOntology);
    OperationDb op = saveOperation(operation);
    return super.create(op, tenant, dtos);
  }

  @Transactional(rollbackFor = Exception.class)
  public OntologyDto updateOntology(
      OperationDb operation, Long tenant, String identifier, OntologyDto dto) {
    checkOntology(dto);
    OperationDb op = saveOperation(operation);
    return super.update(op, tenant, identifier, dto);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteOntology(OperationDb operation, Long tenant, String identifier) {
    // TODO Check if it used
    OperationDb op = saveOperation(operation);
    super.delete(op, tenant, identifier);
  }

  private void checkOntology(OntologyDto ontology) {
    HashSet<String> srcSet = new HashSet<>();
    HashSet<String> dstSet = new HashSet<>();

    for (Mapping mapping : ontology.getMappings()) {
      String src = mapping.src();
      String dst = mapping.dst();

      if (!srcSet.add(src.toLowerCase())) {
        throw new BadRequestException(
            CHECK_ONTOLOGY_FAILED,
            String.format(
                "The source field '%s' in ontology '%s' is duplicated",
                src, ontology.getIdentifier()));
      }

      if (!dstSet.add(dst)) {
        throw new BadRequestException(
            CHECK_ONTOLOGY_FAILED,
            String.format(
                "The destination field '%s' in ontology '%s' is duplicated",
                dst, ontology.getIdentifier()));
      }

      if (FieldUtils.isNotAlphaNumeric(src)) {
        throw new BadRequestException(
            CHECK_ONTOLOGY_FAILED,
            String.format(
                "The source field '%s' in ontology '%s' must only contain letters or digits",
                src, ontology.getIdentifier()));
      }

      if (ArchiveUnitIndex.containsFieldName(src)) {
        throw new BadRequestException(
            CHECK_ONTOLOGY_FAILED,
            String.format(
                "The source field '%s' in ontology '%s' already exists in index mapping",
                src, ontology.getIdentifier()));
      }

      if (!ArchiveUnitIndex.INSTANCE.isExtField(dst)) {
        throw new BadRequestException(
            CHECK_ONTOLOGY_FAILED,
            String.format(
                "The destination field '%s' in ontology '%s' is not a valid destination field",
                dst, ontology.getIdentifier()));
      }
    }
  }

  // Internal use
  public Map<String, String> getMappings(Long tenant, String identifier) {
    return getOptionalEntity(tenant, identifier).map(OntologyDb::getMappings).orElse(null);
  }

  public OntologyMapper createMapper(Long tenant) {
    return new OntologyMapper(this, tenant);
  }

  public SearchResult<OntologyDto> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createOntologyParser(tenant, entityManager), query);
  }
}
