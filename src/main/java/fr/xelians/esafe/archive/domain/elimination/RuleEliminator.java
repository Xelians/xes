/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.elimination;

import fr.xelians.esafe.admin.domain.report.ArchiveReporter;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.rules.computed.AppraisalComputedRules;
import fr.xelians.esafe.archive.domain.unit.rules.computed.HoldComputedRules;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.storage.service.StorageService;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/*
 * @author Emmanuel Deviller
 */
public class RuleEliminator extends Eliminator {

  private final LocalDate eliminationDate;

  public RuleEliminator(
      SearchService searchService, StorageService storageService, TenantDb tenantDb) {
    this(searchService, storageService, tenantDb, null, null, null, null);
  }

  public RuleEliminator(
      SearchService searchService,
      StorageService storageService,
      TenantDb tenantDb,
      AccessContractDb accessContract,
      Path path,
      List<ArchiveUnit> selectedUnits,
      LocalDate eliminationDate) {
    super(searchService, storageService, tenantDb, accessContract, path, selectedUnits);
    this.eliminationDate = eliminationDate;
  }

  @Override
  protected void checkUnit(ArchiveUnit unit) {
    checkAppraisalRule(unit, eliminationDate);
    checkHoldRule(unit, eliminationDate);
  }

  @Override
  protected ArchiveReporter createReporter(Path reportPath, OperationDb operation)
      throws IOException {
    return new ArchiveReporter(ReportType.ELIMINATION, ReportStatus.OK, operation, reportPath);
  }

  private static void checkAppraisalRule(ArchiveUnit unit, LocalDate eliminationDate) {
    AppraisalComputedRules rule = getAppraisalComputedRules(unit);
    LocalDate maxEndDate = rule.getMaxEndDate();
    if (maxEndDate == null || !maxEndDate.isBefore(eliminationDate)) {
      throw new BadRequestException(
          String.format(
              "Cannot eliminate '%s' archive unit with '%s' appraisal max end date ",
              unit.getId(), Objects.toString(rule.getMaxEndDate(), "undefined")));
    }
  }

  private static @NotNull AppraisalComputedRules getAppraisalComputedRules(ArchiveUnit unit) {
    AppraisalComputedRules rule = unit.getComputedInheritedRules().getAppraisalComputedRules();
    if (rule == null) {
      throw new BadRequestException(
          String.format("Cannot eliminate '%s' archive unit without appraisal rule", unit.getId()));
    }

    if (!"Destroy".equalsIgnoreCase(rule.getFinalAction())) {
      throw new BadRequestException(
          String.format(
              "Cannot eliminate '%s' archive unit with appraisal final action %s",
              unit.getId(), rule.getFinalAction()));
    }
    return rule;
  }

  private static void checkHoldRule(ArchiveUnit unit, LocalDate eliminationDate) {
    HoldComputedRules rule = unit.getComputedInheritedRules().getHoldComputedRules();
    if (rule != null) {
      LocalDate maxEndDate = rule.getMaxEndDate();
      if (maxEndDate == null || !maxEndDate.isBefore(eliminationDate)) {
        throw new BadRequestException(
            String.format(
                "Cannot eliminate '%s' archive unit with '%s' hold max end date",
                unit.getId(), Objects.toString(rule.getMaxEndDate(), "undefined")));
      }
    }
  }
}
