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
    dtos.forEach(dto -> checkDuration(dto.getDuration()));
    return super.create(tenant, userIdentifier, applicationId, dtos);
  }

  @Override
  @Transactional
  public RuleDto update(
      Long tenant, String userIdentifier, String applicationId, String identifier, RuleDto dto) {
    checkDuration(dto.getDuration());
    return super.update(tenant, userIdentifier, applicationId, identifier, dto);
  }

  // Beware: it's possible to get unpredictable results if multiple transactions are run at same
  // time
  // on the same tenant. Locking the full table would be very costly for a corner case.
  @Transactional
  public void createRuleCsv(Long tenant, String userIdentifier, String applicationId, String csv) {
    Assert.hasText(csv, "csv cannot be null or empty");

    // delete all rules
    repository.findByTenant(tenant).stream().map(RuleDb::getId).forEach(repository::deleteById);

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
              RULE_CREATION_FAILED, "Failed to parse Rule Csv - Wrong number of records");
        }

        if (StringUtils.isBlank(col.get("RuleId"))) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to parse Rule Csv - Empty RuleId");
        }

        if (StringUtils.isBlank(col.get("RuleValue"))) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to parse Rule Csv - Empty RuleValue");
        }

        if (StringUtils.isBlank(col.get("RuleType"))) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to parse Rule Csv - Empty RuleType");
        }

        if (StringUtils.isBlank(col.get("RuleMeasurement"))) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to parse Rule Csv - Empty RuleMeasurement");
        }

        checkDuration(col.get("RuleDuration"));

        RuleDto rule = new RuleDto();
        rule.setIdentifier(col.get("RuleId"));
        rule.setName(col.get("RuleValue"));
        rule.setDescription(col.get("RuleDescription"));
        rule.setType(RuleType.valueOf(col.get("RuleType")));
        rule.setDuration(col.get("RuleDuration"));
        rule.setMeasurement(RuleMeasurement.valueOf(col.get("RuleMeasurement").toUpperCase()));

        rules.add(rule);
      }
    } catch (IllegalArgumentException | IOException e) {
      throw new BadRequestException(RULE_CREATION_FAILED, "Failed to parse Rule Csv", e);
    }
    return rules;
  }

  private void checkDuration(String duration) {
    if (StringUtils.isBlank(duration)) {
      throw new BadRequestException(
          CHECK_RULE_DURATION_FAILED, "Failed to parse Rule Csv - Empty RuleDuration");
    }

    if (StringUtils.isNumeric(duration)) {
      if (Integer.parseInt(duration) > 9000) {
        throw new BadRequestException(
            CHECK_RULE_DURATION_FAILED,
            String.format("Failed to parse Rule Csv - Rule duration %s is too large", duration));
      }
    } else if (!"unlimited".equalsIgnoreCase(duration)) {
      throw new BadRequestException(
          CHECK_RULE_DURATION_FAILED,
          String.format("Failed to parse Rule Csv - unknown rule duration %s ", duration));
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
