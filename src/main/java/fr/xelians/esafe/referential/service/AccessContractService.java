/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.authentication.domain.AuthContext;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.SipUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.AccessContractDto;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.repository.AccessContractRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
public class AccessContractService
    extends AbstractReferentialService<AccessContractDto, AccessContractDb> {

  private final AgencyService agencyService;
  private final SearchService searchService;

  @Autowired
  public AccessContractService(
      EntityManager entityManager,
      AccessContractRepository repository,
      OperationService operationService,
      AgencyService agencyService,
      SearchService searchService) {

    super(entityManager, repository, operationService);
    this.agencyService = agencyService;
    this.searchService = searchService;
  }

  @Override
  @Transactional
  public List<AccessContractDto> create(
      Long tenant, String userIdentifier, String applicationId, List<AccessContractDto> dtos) {
    dtos.forEach(this::checkAccessContract);
    return super.create(tenant, userIdentifier, applicationId, dtos);
  }

  @Override
  @Transactional
  public AccessContractDto update(
      Long tenant,
      String userIdentifier,
      String applicationId,
      String identifier,
      AccessContractDto dto) {
    checkAccessContract(dto);
    return super.update(tenant, userIdentifier, applicationId, identifier, dto);
  }

  private void checkAccessContract(AccessContractDto dto) {
    checkRootUnits(dto);
    checkExcludedRootUnits(dto);
    checkDataObjectVersion(dto);
    checkAgencies(dto);
  }

  private void checkRootUnits(AccessContractDto dto) {
    Set<Long> rootUnits = dto.getRootUnits();

    if (rootUnits != null) {
      try {
        for (Long rootUnit : rootUnits) {
          if (!searchService.existsById(AuthContext.getTenant(), rootUnit)) {
            throw new NotFoundException(
                "Check root units failed",
                String.format(
                    "Access Contract %s - tenant %s  - Root unit %s not found",
                    dto.getName(), AuthContext.getTenant(), rootUnit));
          }
        }
      } catch (IOException ex) {
        throw new InternalException(
            "Check root units failed",
            String.format(
                "Access Contract %s - tenant %s  - Failed to check root units ",
                dto.getName(), AuthContext.getTenant()),
            ex);
      }
    }
  }

  private void checkExcludedRootUnits(AccessContractDto dto) {
    Set<Long> excludedRootUnits = dto.getExcludedRootUnits();

    if (excludedRootUnits != null) {
      try {
        for (Long rootUnit : excludedRootUnits) {
          if (!searchService.existsById(AuthContext.getTenant(), rootUnit)) {
            throw new NotFoundException(
                "Check exclude root units failed",
                String.format(
                    "Access Contract: %s - tenant: %s  - Excluded root unit %s not found",
                    dto.getName(), AuthContext.getTenant(), rootUnit));
          }
        }
      } catch (IOException ex) {
        throw new InternalException(
            "Check exclude root units failed",
            String.format(
                "Access Contract: %s - tenant: %s  - Failed to check excluded root units ",
                dto.getName(), AuthContext.getTenant()),
            ex);
      }
    }
  }

  private void checkDataObjectVersion(AccessContractDto dto) {
    Set<String> qualifiers = dto.getDataObjectVersion();

    if (Utils.isFalse(dto.getEveryDataObjectVersion())
        && (qualifiers == null || qualifiers.isEmpty())) {
      throw new BadRequestException(
          "Check data object version failed",
          String.format(
              "Access Contract: %s - tenant: %s - Qualifier (data object version) list is null or empty",
              dto.getName(), AuthContext.getTenant()));
    }

    if (qualifiers != null) {
      for (String qualifier : qualifiers) {
        if (!SipUtils.isValidQualifier(qualifier)) {
          throw new BadRequestException(
              "Check data object version failed",
              String.format(
                  "Access Contract: %s - tenant: %s - Bad qualifier (data object version) %s",
                  dto.getName(), AuthContext.getTenant(), qualifier));
        }
      }
    }
  }

  private void checkAgencies(AccessContractDto dto) {
    Set<String> agencies = dto.getOriginatingAgencies();

    if (Utils.isFalse(dto.getEveryOriginatingAgency())
        && (agencies == null || agencies.isEmpty())) {
      throw new BadRequestException(
          "Check agencies failed",
          String.format(
              "Access Contract: %s - tenant: %s  - Agency list is null or empty",
              dto.getName(), AuthContext.getTenant()));
    }

    if (agencies != null) {
      for (String agencyIdentifier : agencies) {
        if (!agencyService.existsByIdentifier(agencyIdentifier)) {
          throw new NotFoundException(
              "Check agencies failed",
              String.format(
                  "Access Contract: %s - tenant: %s  - Agency identifier %s not found",
                  dto.getName(), AuthContext.getTenant(), agencyIdentifier));
        }
      }
    }
  }

  public SearchResult<JsonNode> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createAccessContractParser(tenant, entityManager), query);
  }
}
