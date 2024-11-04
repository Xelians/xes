/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import static java.util.stream.Collectors.*;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import de.siegmar.fastcsv.reader.*;
import fr.xelians.esafe.admin.domain.report.RulesReporter;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitIndex;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.NioUtils;
import fr.xelians.esafe.operation.domain.ActionType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.domain.Workspace;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.RuleMeasurement;
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.AbstractReferentialDto;
import fr.xelians.esafe.referential.dto.RuleDto;
import fr.xelians.esafe.referential.entity.RuleDb;
import fr.xelians.esafe.referential.repository.RuleRepository;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.service.StorageService;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
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
public class RuleService extends AbstractReferentialService<RuleDto, RuleDb> {

  private static final List<String> CSV_HEADER =
      List.of(
          "RuleId", "RuleType", "RuleValue", "RuleDescription", "RuleDuration", "RuleMeasurement");

  public static final String CHECK_RULE_DURATION_FAILED = "Check rule duration failed";
  public static final String RULE_CREATION_FAILED = "Rule creation failed";
  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ID_MUST_BE_NOT_NULL = "id must be not null";

  private final TenantService tenantService;
  private final StorageService storageService;
  private final SearchEngineService searchEngineService;

  @Autowired
  public RuleService(
      EntityManager entityManager,
      RuleRepository repository,
      OperationService operationService,
      TenantService tenantService,
      StorageService storageService,
      SearchEngineService searchEngineService) {
    super(entityManager, repository, operationService, tenantService);
    this.tenantService = tenantService;
    this.storageService = storageService;
    this.searchEngineService = searchEngineService;
  }

  // Beware: it's possible to get unpredictable results if multiple transactions are run at same
  // time on the same tenant. Locking the full table would be very costly for a corner case.
  @Transactional(rollbackFor = Exception.class)
  public List<RuleDto> createCsvRules(OperationDb operation, Long tenant, String csv) {
    Assert.hasText(csv, "csv cannot be null or empty");

    List<RuleDto> createList = new ArrayList<>();
    List<RuleDto> updateList = new ArrayList<>();

    // Get all rules
    Map<String, RuleDto> rules =
        repository.findByTenant(tenant).stream()
            .map(this::toDto)
            .collect(toMap(RuleDto::getIdentifier, Function.identity()));

    for (RuleDto agency : fromCsv(csv)) {
      String identifier = agency.getIdentifier();
      if (rules.containsKey(identifier)) {
        updateList.add(agency);
      } else {
        createList.add(agency);
      }
      rules.remove(identifier);
    }

    // Create new rules
    OperationDb op = saveOperation(operation);
    List<RuleDto> ruleDtos = super.create(op, tenant, createList);

    // Update existing rules
    for (RuleDto rule : updateList) {
      RuleDto ruleDto = super.update(op, tenant, rule.getIdentifier(), rule);
      ruleDtos.add(ruleDto);
    }

    // Inactive remaining rules
    for (RuleDto rule : rules.values()) {
      rule.setStatus(Status.INACTIVE);
      super.update(op, tenant, rule.getIdentifier(), rule);
    }

    List<String> insertedIds =
        ruleDtos.stream().map(AbstractReferentialDto::getIdentifier).toList();
    List<String> deletedIds =
        rules.values().stream().map(AbstractReferentialDto::getIdentifier).toList();

    // Write report
    writeReport(op, insertedIds, deletedIds);
    return ruleDtos;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<RuleDto> createRules(OperationDb operation, Long tenant, List<RuleDto> dtos) {

    dtos.forEach(
        dto ->
            checkDuration(
                dto.getIdentifier(),
                dto.getType(),
                dto.getDuration(),
                dto.getMeasurement().toString()));

    OperationDb op = saveOperation(operation);
    return super.create(op, tenant, dtos);
  }

  @Transactional(rollbackFor = Exception.class)
  public RuleDto updateRule(OperationDb operation, Long tenant, String identifier, RuleDto dto) {
    checkDuration(
        dto.getIdentifier(), dto.getType(), dto.getDuration(), dto.getMeasurement().toString());

    OperationDb op = saveOperation(operation);
    return super.update(op, tenant, identifier, dto);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteRule(OperationDb operation, Long tenant, String identifier) throws IOException {
    RuleDb rule = getEntity(tenant, identifier);

    LocalDate lastUpdate = rule.getLastUpdate();
    LocalDate now = LocalDate.now();

    if (rule.getStatus() == Status.INACTIVE && now.isAfter(lastUpdate.plusDays(0))) {
      checkDeletedRule(tenant, rule.getType(), rule.getIdentifier());
      OperationDb op = saveOperation(operation);
      super.delete(op, tenant, identifier);
    } else {
      throw new BadRequestException(
          "Failed to delete rules",
          String.format(
              "The '%s' rule '%s' must be inactive for at least one day",
              rule.getStatus(), rule.getIdentifier()));
    }
  }

  public InputStream getRuleReport(Long tenant, Long id) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      return storageDao.getReportStream(tenant, offers, id);
    }
  }

  private void checkDeletedRule(Long tenant, RuleType ruleType, String identifier)
      throws IOException {

    String field = "_mgt." + ruleType + ".Rules.Rule";

    Query ruleQuery =
        TermQuery.of(t -> t.field(field).value(v -> v.stringValue(identifier)))._toQuery();
    Query tenantQuery =
        TermQuery.of(t -> t.field("_tenant").value(v -> v.longValue(tenant)))._toQuery();
    Query boolQuery = BoolQuery.of(b -> b.must(ruleQuery).filter(tenantQuery))._toQuery();
    SourceConfig config = SourceConfig.of(s -> s.filter(f -> f.includes("_unitId")));

    // Create search request
    SearchRequest request =
        SearchRequest.of(
            r -> r.index(ArchiveUnitIndex.ALIAS).size(1).query(boolQuery).source(config));
    SearchResponse<JsonNode> response = searchEngineService.search(request, JsonNode.class);

    int size = response.hits().hits().size();
    if (size == 1) {
      throw new BadRequestException(
          "Failed to update rules",
          String.format(
              "Some deleted rules are still used in archive unit '%s'",
              response.hits().hits().getFirst().source().get("_unitId")));
    }
  }

  private void writeReport(OperationDb operation, List<String> inserted, List<String> deleted) {

    TenantDb tenantDb = tenantService.getTenantDb(operation.getTenant());
    List<String> offers = tenantDb.getStorageOffers();
    Path reportPath = null;

    // Write report on offers
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      reportPath = Workspace.createTempFile(operation);
      RulesReporter.write(operation, reportPath, inserted, deleted);
      List<StorageObject> psois =
          List.of(new PathStorageObject(reportPath, operation.getId(), StorageObjectType.rep));
      storageDao
          .putStorageObjects(operation.getTenant(), offers, psois)
          .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));

    } catch (IOException ex) {
      throw new InternalException(ex);
    } finally {
      NioUtils.deleteDirQuietly(reportPath);
    }
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

  private static List<RuleDto> fromCsv(String csv) {

    boolean firsTime = true;
    List<RuleDto> rules = new ArrayList<>();

    try (CsvReader<NamedCsvRecord> records =
        CsvReader.builder()
            .skipEmptyLines(false)
            .build(new NamedCsvRecordHandler(FieldModifiers.TRIM), csv)) {

      for (var rec : records) {

        if (firsTime) {
          if (!CSV_HEADER.equals(rec.getHeader())) {
            throw new BadRequestException(
                RULE_CREATION_FAILED, "Failed to parse Rule Csv - Bad header");
          }
          firsTime = false;
        }

        if (rec.getFields().size() != 6) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to parse csv - Wrong number of records");
        }

        String ruleId = rec.getField("RuleId");
        if (StringUtils.isBlank(ruleId)) {
          throw new BadRequestException(
              RULE_CREATION_FAILED, "Failed to validate Rule - Empty RuleId");
        }

        String ruleValue = rec.getField("RuleValue");
        if (StringUtils.isBlank(ruleValue)) {
          throw new BadRequestException(
              RULE_CREATION_FAILED,
              String.format("Failed to validate Rule '%s' - Empty RuleValue", ruleId));
        }

        String ruleType = rec.getField("RuleType");
        if (StringUtils.isBlank(ruleType)) {
          throw new BadRequestException(
              RULE_CREATION_FAILED,
              String.format("Failed to validate Rule '%s' - Empty RuleType", ruleId));
        }

        String duration = rec.getField("RuleDuration");
        String measurement = rec.getField("RuleMeasurement");
        checkDuration(ruleId, RuleType.valueOf(ruleType), duration, measurement);

        // Defaults for empty hold rule duration
        if (StringUtils.isBlank(duration)) duration = "unlimited";
        if (StringUtils.isBlank(measurement)) measurement = RuleMeasurement.YEAR.toString();

        RuleDto rule = new RuleDto();
        rule.setIdentifier(ruleId);
        rule.setName(ruleValue);
        rule.setDescription(rec.getField("RuleDescription"));
        rule.setType(RuleType.valueOf(ruleType));
        rule.setDuration(duration);
        rule.setMeasurement(RuleMeasurement.valueOf(measurement.toUpperCase()));

        rules.add(rule);
      }
    } catch (IllegalArgumentException | CsvParseException | UncheckedIOException | IOException e) {
      throw new BadRequestException(RULE_CREATION_FAILED, "Failed to parse Rule Csv", e);
    }
    return rules;
  }

  private static void checkDuration(
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

  public String getCsvRules(final long tenant) {
    return String.join(",", CSV_HEADER)
        + "\n"
        + repository.findByTenant(tenant).stream()
            .map(this::toCsv)
            .collect(Collectors.joining("\n"));
  }

  public String getRulesByIdentifier(String identifier, Long tenant) {
    Assert.hasText(identifier, "identifier cannot be null or empty");

    return String.join(",", CSV_HEADER)
        + "\n"
        + repository
            .findByTenantAndIdentifier(tenant, identifier)
            .map(this::toCsv)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rule not found", String.format("Rule with is %s not found", identifier)));
  }

  public SearchResult<RuleDto> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createRuleParser(tenant, entityManager), query);
  }
}
