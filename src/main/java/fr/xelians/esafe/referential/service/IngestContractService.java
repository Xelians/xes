/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.DroidUtils;
import fr.xelians.esafe.common.utils.SipUtils;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.CheckParentLinkStatus;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.IngestContractDto;
import fr.xelians.esafe.referential.entity.IngestContractDb;
import fr.xelians.esafe.referential.repository.IngestContractRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
public class IngestContractService
    extends AbstractReferentialService<IngestContractDto, IngestContractDb> {

  public static final String CHECK_FORMATS_FAILED = "Check formats failed";
  private final ProfileService profileService;
  private final SearchService searchService;

  @Autowired
  public IngestContractService(
      EntityManager entityManager,
      IngestContractRepository repository,
      OperationService operationService,
      TenantService tenantService,
      ProfileService profileService,
      SearchService searchService) {
    super(entityManager, repository, operationService, tenantService);
    this.profileService = profileService;
    this.searchService = searchService;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<IngestContractDto> createIngestContracts(
      OperationDb operation, Long tenant, List<IngestContractDto> dtos) {
    dtos.forEach(dto -> checkIngestContract(tenant, dto));
    OperationDb op = saveOperation(operation);
    return super.create(op, tenant, dtos);
  }

  @Transactional(rollbackFor = Exception.class)
  public IngestContractDto updateIngestContract(
      OperationDb operation, Long tenant, String identifier, IngestContractDto dto) {
    checkIngestContract(tenant, dto);
    OperationDb op = saveOperation(operation);
    return super.update(op, tenant, identifier, dto);
  }

  private void checkIngestContract(Long tenant, IngestContractDto dto) {
    checkQualifiers(dto);
    checkFormats(dto);
    checkProfiles(dto);
    checkLinkParentId(tenant, dto);
  }

  private void checkQualifiers(IngestContractDto dto) {
    Set<String> qualifiers = dto.getDataObjectVersion();

    if (qualifiers != null) {
      for (String qualifier : qualifiers) {
        if (!SipUtils.isValidQualifier(qualifier)) {
          throw new BadRequestException(
              "Check qualifiers failed",
              String.format(
                  "Ingest Contract '%s' - Bad qualifier (data object version) %s ",
                  dto.getName(), qualifier));
        }
      }
    }
  }

  private void checkFormats(IngestContractDto dto) {
    Set<String> formats = dto.getFormatType();

    if (BooleanUtils.isTrue(dto.getEveryFormatType())) {
      if (formats != null && !formats.isEmpty()) {
        throw new BadRequestException(
            CHECK_FORMATS_FAILED,
            String.format(
                "Ingest Contract '%s' - Every data format is not compatible with non null format list",
                dto.getName()));
      }
    } else {
      if (formats == null || formats.isEmpty()) {
        throw new BadRequestException(
            CHECK_FORMATS_FAILED,
            String.format("Ingest Contract '%s' - Format list is null or empty", dto.getName()));
      }

      for (String puid : formats) {
        if (!DroidUtils.isSupportedFormat(puid)) {
          throw new BadRequestException(
              CHECK_FORMATS_FAILED,
              String.format(
                  "Ingest Contract '%s' - Not supported (puid) file format %s",
                  dto.getName(), puid));
        }
      }
    }
  }

  private void checkProfiles(IngestContractDto dto) {
    Set<String> archiveProfiles = dto.getArchiveProfiles();

    if (archiveProfiles != null) {
      for (String profileIdentifier : archiveProfiles) {
        if (!profileService.existsByIdentifier(profileIdentifier)) {
          throw new NotFoundException(
              "Check profiles failed",
              String.format(
                  "Ingest Contract %s - Profile identifier %s not found in tenant",
                  dto.getName(), profileIdentifier));
        }
      }
    }
  }

  private void checkLinkParentId(Long tenant, IngestContractDto dto) {
    Long linkParentId = dto.getLinkParentId();

    if (linkParentId != null) {
      if (dto.getCheckParentLink() == CheckParentLinkStatus.REQUIRED) {
        throw new BadRequestException(
            "Update operation mismatch",
            String.format(
                "Ingest Contract '%s' - Link parent must not be specified if update operation is required",
                dto.getName()));
      }
      try {
        if (!searchService.existsById(tenant, linkParentId)) {
          throw new NotFoundException(
              "Link parent not found",
              String.format(
                  "Ingest Contract '%s' - Link parent unit %s not found",
                  dto.getName(), linkParentId));
        }
      } catch (IOException ex) {
        throw new InternalException(
            "Check link parent failed",
            String.format(
                "Ingest Contract '%s' - Failed to check link parent unit ", dto.getName()),
            ex);
      }
    }
  }

  public SearchResult<IngestContractDto> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createIngestContractParser(tenant, entityManager), query);
  }

  @Override
  protected String getIdentifierPrefix() {
    return "IC";
  }
}
