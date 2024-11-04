/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import static java.util.stream.Collectors.toMap;

import de.siegmar.fastcsv.reader.*;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.AgencyDto;
import fr.xelians.esafe.referential.entity.AgencyDb;
import fr.xelians.esafe.referential.repository.AgencyRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
public class AgencyService extends AbstractReferentialService<AgencyDto, AgencyDb> {

  private static final List<String> CSV_HEADER = List.of("Identifier", "Name", "Description");
  public static final String AGENCY_CREATION_FAILED = "Agency creation failed";

  @Autowired
  public AgencyService(
      EntityManager entityManager,
      AgencyRepository repository,
      OperationService operationService,
      TenantService tenantService) {
    super(entityManager, repository, operationService, tenantService);
  }

  @Transactional(rollbackFor = Exception.class)
  public List<AgencyDto> createAgencies(OperationDb operation, Long tenant, List<AgencyDto> dtos) {
    OperationDb op = saveOperation(operation);
    return super.create(op, tenant, dtos);
  }

  @Transactional(rollbackFor = Exception.class)
  public AgencyDto updateAgency(
      OperationDb operation, Long tenant, String identifier, AgencyDto dto) {
    OperationDb op = saveOperation(operation);
    return super.update(op, tenant, identifier, dto);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteAgency(OperationDb operation, Long tenant, String identifier) {
    // TODO check if used
    OperationDb op = saveOperation(operation);
    super.delete(op, tenant, identifier);
  }

  // Beware: it's possible to get unpredictable results if multiple transactions are run at same
  // time
  // on the same tenant. Locking the full table would be very costly for a corner case.
  @Transactional(rollbackFor = Exception.class)
  public List<AgencyDto> createCsvAgencies(OperationDb operation, Long tenant, String csv) {
    Assert.hasText(csv, "csv cannot be null or empty");

    List<AgencyDto> createList = new ArrayList<>();
    List<AgencyDto> updateList = new ArrayList<>();

    // Get all agencies
    Map<String, AgencyDto> agencies =
        repository.findByTenant(tenant).stream()
            .map(this::toDto)
            .collect(toMap(AgencyDto::getIdentifier, Function.identity()));

    for (AgencyDto agency : fromCsv(csv)) {
      String identifier = agency.getIdentifier();
      if (agencies.containsKey(identifier)) {
        updateList.add(agency);
      } else {
        createList.add(agency);
      }
      agencies.remove(identifier);
    }

    // Create new agencies
    OperationDb op = saveOperation(operation);
    List<AgencyDto> agencyDtos = super.create(op, tenant, createList);

    // Update existing agencies
    for (AgencyDto agency : updateList) {
      AgencyDto agencyDto = super.update(op, tenant, agency.getIdentifier(), agency);
      agencyDtos.add(agencyDto);
    }

    // Inactive remaining agencies
    for (AgencyDto agency : agencies.values()) {
      agency.setStatus(Status.INACTIVE);
      super.update(op, tenant, agency.getIdentifier(), agency);
    }

    return agencyDtos;
  }

  public String getAgenciesCsv(Long tenant) {
    return String.join(",", CSV_HEADER)
        + "\n"
        + repository.findByTenant(tenant).stream()
            .map(AgencyService::toCsv)
            .collect(Collectors.joining("\n"));
  }

  public String getAgenciesCsv(Long tenant, String identifier) {
    Assert.hasText(identifier, "identifier cannot be null or empty");
    return String.join(",", CSV_HEADER) + "\n" + toCsv(getEntity(tenant, identifier));
  }

  private static String toCsv(AgencyDb agency) {
    return agency.getIdentifier()
        + ",\""
        + agency.getName()
        + "\",\""
        + StringUtils.defaultString(agency.getDescription())
        + "\"";
  }

  private static List<AgencyDto> fromCsv(String csv) {

    boolean firsTime = true;
    Set<AgencyDto> agencies = new HashSet<>();

    try (CsvReader<NamedCsvRecord> records =
        CsvReader.builder()
            .skipEmptyLines(false)
            .build(new NamedCsvRecordHandler(FieldModifiers.TRIM), csv)) {

      for (var rec : records) {
        if (firsTime) {
          if (!CSV_HEADER.equals(rec.getHeader())) {
            throw new BadRequestException(
                AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Bad header");
          }
          firsTime = false;
        }

        if (rec.getFields().size() != 3) {
          throw new BadRequestException(
              AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Wrong number of records");
        }

        String identifier = rec.getField("Identifier");
        if (StringUtils.isBlank(identifier)) {
          throw new BadRequestException(
              AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Empty Identifier");
        }

        String name = rec.getField("Name");
        if (StringUtils.isBlank(name)) {
          throw new BadRequestException(
              AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Empty Name");
        }

        AgencyDto agency = new AgencyDto();
        agency.setIdentifier(identifier);
        agency.setName(name);
        agency.setDescription(rec.getField("Description"));
        agencies.add(agency);
      }
    } catch (IllegalArgumentException | CsvParseException | UncheckedIOException | IOException e) {
      throw new BadRequestException(
          AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - message: ", e);
    }
    return new ArrayList<>(agencies);
  }

  public SearchResult<AgencyDto> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createAgencyParser(tenant, entityManager), query);
  }

  @Override
  protected String getIdentifierPrefix() {
    return "AG";
  }
}
