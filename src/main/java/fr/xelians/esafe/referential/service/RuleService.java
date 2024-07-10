/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates and open the template
 * in the editor.
 */
/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.referential.domain.RuleMeasurement;
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.RuleDto;
import fr.xelians.esafe.referential.entity.RuleDb;
import fr.xelians.esafe.referential.repository.RuleRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
public class RuleService extends AbstractReferentialService<RuleDto, RuleDb> {

  private static final String CSV_HEADER =
      "RuleId,RuleType,RuleValue,RuleDescription,RuleDuration,RuleMeasurement";
  public static final String CHECK_RULE_DURATION_FAILED = "Check rule duration failed";
  public static final String RULE_CREATION_FAILED = "Rule creation failed";

  @Autowired
  public RuleService(
      EntityManager entityManager, RuleRepository repository, OperationService operationService) {
    super(entityManager, repository, operationService);
  }

  @Override
  @Transactional
  public List<RuleDto> create(
      Long tenant, String userIdentifier, String applicationId, List<RuleDto> dtos) {
    dtos.forEach(
        dto ->
            checkDuration(
                dto.getIdentifier(),
                dto.getType(),
                dto.getDuration(),
                dto.getMeasurement().toString()));
    return super.create(tenant, userIdentifier, applicationId, dtos);
  }

  @Override
  @Transactional
  public RuleDto update(
      Long tenant, String userIdentifier, String applicationId, String identifier, RuleDto dto) {
    checkDuration(
        dto.getIdentifier(), dto.getType(), dto.getDuration(), dto.getMeasurement().toString());
    return super.update(tenant, userIdentifier, applicationId, identifier, dto);
  }

  // Beware: it's possible to get unpredictable results if multiple transactions are run at same
  // time
  // on the same tenant. Locking the full table would be very costly for a corner case.
  @Transactional
  public void createRuleCsv(Long tenant, String userIdentifier, String applicationId, String csv) {
    Assert.hasText(csv, "csv cannot be null or empty");

    // delete all rules
    repository.deleteWithTenant(tenant);

    // create new rule
    super.create(tenant, userIdentifier, applicationId, fromCsv(csv));
  }

  private String toCsv(RuleDb rule) {
    return rule.getIdentifier()
        + ","
        + rule.getType()
        + ",\""
        + rule.getName()
        + "\",\""
        + StringUtils.defaultString(rule.getDescription())
        + "\","
        + rule.getDuration()
        + ","
        + rule.getMeasurement();
  }

  private List<RuleDto> fromCsv(String csv) {
    ArrayList<RuleDto> rules = new ArrayList<>();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setDelimiter(',')
            .setIgnoreEmptyLines(false)
            .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setQuote('"')
            .build();

    try (CSVParser parser = CSVParser.parse(csv, csvFormat)) {
      String headers = String.join(",", parser.getHeaderNames());
      if (!CSV_HEADER.equals(headers)) {
        throw new BadRequestException(
            RULE_CREATION_FAILED, "Failed to parse Rule Csv - Bad header");
      }

      for (CSVRecord col : parser) {
        if (col.size() != 6) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to parse csv - Wrong number of records");
        }

        String ruleId = col.get("RuleId");
        if (StringUtils.isBlank(ruleId)) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to validate Rule - Empty RuleId");
        }

        String ruleValue = col.get("RuleValue");
        if (StringUtils.isBlank(ruleValue)) {
          throw new BadRequestException(
              RULE_CREATION_FAILED,
              String.format("Failed to validate Rule '%s' - Empty RuleValue", ruleId));
        }

        String ruleType = col.get("RuleType");
        if (StringUtils.isBlank(ruleType)) {
          throw new BadRequestException(
              RULE_CREATION_FAILED,
              String.format("Failed to validate Rule '%s' - Empty RuleType", ruleId));
        }

        String duration = col.get("RuleDuration");
        String measurement = col.get("RuleMeasurement");
        checkDuration(ruleId, RuleType.valueOf(ruleType), duration, measurement);

        // Defaults for empty hold rule duration
        if (StringUtils.isBlank(duration)) duration = "unlimited";
        if (StringUtils.isBlank(measurement)) measurement = RuleMeasurement.YEAR.toString();

        RuleDto rule = new RuleDto();
        rule.setIdentifier(ruleId);
        rule.setName(ruleValue);
        rule.setDescription(col.get("RuleDescription"));
        rule.setType(RuleType.valueOf(ruleType));
        rule.setDuration(duration);
        rule.setMeasurement(RuleMeasurement.valueOf(measurement.toUpperCase()));

        rules.add(rule);
      }
    } catch (IllegalArgumentException | IOException e) {
      throw new BadRequestException(RULE_CREATION_FAILED, "Failed to parse Rule Csv", e);
    }
    return rules;
  }

  private void checkDuration(
      String ruleId, RuleType ruleType, String duration, String measurement) {
    if (ruleType == RuleType.HoldRule) {
      if (StringUtils.isBlank(duration)) return;
    } else {
      if (StringUtils.isBlank(duration)) {
        throw new BadRequestException(
            CHECK_RULE_DURATION_FAILED,
            String.format("Failed to validate Rule '%s' - Rule Duration is not defined", ruleId));
      }
    }

    if (StringUtils.isNumeric(duration)) {
      if (Integer.parseInt(duration) > 9000) {
        throw new BadRequestException(
            CHECK_RULE_DURATION_FAILED,
            String.format(
                "Failed to validate Rule '%s - Rule duration '%s' is too large", ruleId, duration));
      }
    } else if (!"unlimited".equalsIgnoreCase(duration)) {
      throw new BadRequestException(
          CHECK_RULE_DURATION_FAILED,
          String.format(
              "Failed to validate Rule '%s' - Rule duration '%s' is unknown", ruleId, duration));
    }

    if (StringUtils.isBlank(measurement)) {
      throw new BadRequestException(
          CHECK_RULE_DURATION_FAILED,
          String.format("Failed to validate Rule '%s' - Rule Measurement is not defined", ruleId));
    }
  }

  public String getRulesCsv(final long tenant) {
    return CSV_HEADER
        + "\n"
        + repository.findByTenant(tenant).stream()
            .map(this::toCsv)
            .collect(Collectors.joining("\n"));
  }

  public String getRulesByIdentifier(String identifier, Long tenant) {
    Assert.hasText(identifier, "identifier cannot be null or empty");

    return CSV_HEADER
        + "\n"
        + repository
            .findByTenantAndIdentifier(tenant, identifier)
            .map(this::toCsv)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rule not found", String.format("Rule with is %s not found", identifier)));
  }

  public SearchResult<JsonNode> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createRuleParser(tenant, entityManager), query);
  }
}
