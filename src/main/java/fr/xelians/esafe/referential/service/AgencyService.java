/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates and open the template
 * in the editor.
 */
/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.service;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.AgencyDto;
import fr.xelians.esafe.referential.entity.AgencyDb;
import fr.xelians.esafe.referential.repository.AgencyRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
public class AgencyService extends AbstractReferentialService<AgencyDto, AgencyDb> {

  private static final String CSV_HEADER = "Identifier,Name,Description";
  public static final String AGENCY_CREATION_FAILED = "Agency creation failed";

  @Autowired
  public AgencyService(
      EntityManager entityManager, AgencyRepository repository, OperationService operationService) {
    super(entityManager, repository, operationService);
  }

  // Beware: it's possible to get unpredictable results if multiple transactions are run at same
  // time
  // on the same tenant. Locking the full table would be very costly for a corner case.
  @Transactional
  public void createAgencyCsv(
      Long tenant, String userIdentifier, String applicationId, String csv) {
    Assert.hasText(csv, "csv cannot be null or empty");

    List<AgencyDto> createList = new ArrayList<>();
    List<AgencyDto> updateList = new ArrayList<>();
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
    super.create(tenant, userIdentifier, applicationId, createList);

    // Update existing agencies
    for (AgencyDto agency : updateList) {
      super.update(tenant, userIdentifier, applicationId, agency.getIdentifier(), agency);
    }

    // Inactive others agencies (We choose not to delete agency)
    for (AgencyDto agency : agencies.values()) {
      agency.setStatus(Status.INACTIVE);
      super.update(tenant, userIdentifier, applicationId, agency.getIdentifier(), agency);
    }
  }

  public String getAgenciesCsv(Long tenant) {
    return CSV_HEADER
        + "\n"
        + repository.findByTenant(tenant).stream()
            .map(AgencyService::toCsv)
            .collect(Collectors.joining("\n"));
  }

  public String getAgencyCsv(Long tenant, String identifier) {
    Assert.hasText(identifier, "identifier cannot be null or empty");

    return CSV_HEADER + "\n" + toCsv(getEntity(tenant, identifier));
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
    Set<AgencyDto> agencies = new HashSet<>();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setDelimiter(',')
            .setIgnoreEmptyLines(false)
            .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setTrim(true)
            .setQuote('"')
            .build();

    try (CSVParser parser = CSVParser.parse(csv, csvFormat)) {
      String headers = String.join(",", parser.getHeaderNames());
      if (!CSV_HEADER.equals(headers)) {
        throw new BadRequestException(
            AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Bad header");
      }

      for (CSVRecord col : parser) {
        if (col.size() != 3) {
          throw new BadRequestException(
              AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Wrong number of records");
        }

        if (StringUtils.isBlank(col.get("Identifier"))) {
          throw new BadRequestException(
              AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Empty Identifier");
        }

        if (StringUtils.isBlank(col.get("Name"))) {
          throw new BadRequestException(
              AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - Empty Name");
        }

        AgencyDto agency = new AgencyDto();
        agency.setIdentifier(col.get("Identifier"));
        agency.setName(col.get("Name"));
        agency.setDescription(col.get("Description"));
        agencies.add(agency);
      }
    } catch (IOException e) {
      throw new BadRequestException(
          AGENCY_CREATION_FAILED, "Failed to parse Agency Csv - message: ", e);
    }
    return new ArrayList<>(agencies);
  }

  public SearchResult<JsonNode> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createAgencyParser(tenant, entityManager), query);
  }
}
