/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.rules.FinalActionRule;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.archive.domain.unit.rules.computed.*;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
import fr.xelians.esafe.common.exception.functional.ManifestException;
import fr.xelians.esafe.common.utils.SipUtils;
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.RuleDb;
import fr.xelians.esafe.referential.service.RuleService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DateRuleService {

  public static final String CHECK_MANAGEMENT_RULES_FAILED = "Check management rules failed";
  public static final String UNLIMITED = "unlimited";

  private final RuleService ruleService;

  public void setRulesEndDates(
      Long tenant, Map<String, RuleDb> ruleMap, ArchiveUnit childUnit, ArchiveUnit parentUnit) {

    // Compute EndDate in management rules (i.e. without inheritance)
    Management childMgt = childUnit.getManagement();
    if (childMgt != null) {
      computeEndDates(tenant, ruleMap, childMgt);
    }

    // Init default computed inherited rules
    childUnit.initComputedInheritedRules();

    // Compute MaxEndDate in computed inherited rules (i.e. with inheritance)
    if (parentUnit != null) {
      ComputedInheritedRules parentCir = parentUnit.getComputedInheritedRules();
      if (parentCir != null) {
        computeMaxEndDates(childUnit, parentCir);
      }
    }
  }

  public void computeEndDates(Long tenant, Map<String, RuleDb> ruleMap, Management childMgt) {
    computeEndDate(tenant, ruleMap, childMgt.getAppraisalRules());
    computeEndDate(tenant, ruleMap, childMgt.getAccessRules());
    computeEndDate(tenant, ruleMap, childMgt.getDisseminationRules());
    computeEndDate(tenant, ruleMap, childMgt.getReuseRules());
    computeEndDate(tenant, ruleMap, childMgt.getStorageRules());
    computeEndDate(tenant, ruleMap, childMgt.getClassificationRules());
    computeEndDate(tenant, ruleMap, childMgt.getHoldRules());
  }

  // Compute the max inherited dates (note. end dates must be computed before)
  public static void computeMaxEndDates(ArchiveUnit childUnit, ComputedInheritedRules parentCir) {
    computeMaxEndDate(childUnit, parentCir.getAppraisalComputedRules());
    computeMaxEndDate(childUnit, parentCir.getAccessComputedRules());
    computeMaxEndDate(childUnit, parentCir.getDisseminationComputedRules());
    computeMaxEndDate(childUnit, parentCir.getReuseComputedRules());
    computeMaxEndDate(childUnit, parentCir.getStorageComputedRules());
    computeMaxEndDate(childUnit, parentCir.getClassificationComputedRules());
    computeMaxEndDate(childUnit, parentCir.getHoldComputedRules());
  }

  private void computeEndDate(Long tenant, Map<String, RuleDb> ruleMap, AbstractSimpleRules aRule) {
    if (aRule != null) {
      aRule.setEndDate(null); // null means "unlimited"
      if (!aRule.getRules().isEmpty()) {
        boolean unlimited = false;
        RuleType ruleType = aRule.getRuleType();
        for (Rule rule : aRule.getRules()) {
          LocalDate ruleDate = getRuleDate(tenant, ruleMap, rule, ruleType);
          rule.setEndDate(ruleDate);
          if (ruleDate == null) {
            unlimited = true;
            aRule.setEndDate(null);
            // Don't break here because we need  to compute all ruleDate
          } else if (!unlimited && isAfter(ruleDate, aRule.getEndDate())) {
            aRule.setEndDate(ruleDate);
          }
        }
      }
    }
  }

  private static boolean isAfter(LocalDate date1, LocalDate date2) {
    return date2 == null || date1.isAfter(date2);
  }

  private void computeEndDate(Long tenant, Map<String, RuleDb> ruleMap, HoldRules aRule) {
    if (aRule != null) {
      aRule.setEndDate(null); // null means "unlimited"
      if (!aRule.getHoldRules().isEmpty()) {
        boolean unlimited = false;
        for (HoldRule rule : aRule.getHoldRules()) {
          LocalDate ruleDate = getRuleDate(tenant, ruleMap, rule, RuleType.HoldRule);
          if (rule.getStartDate() != null && rule.getHoldEndDate() != null) {
            ruleDate = rule.getHoldEndDate();
          }
          rule.setEndDate(ruleDate);
          if (ruleDate == null) {
            unlimited = true;
            aRule.setEndDate(null);
            // Don't break here because we need to compute all ruleDate
          } else if (!unlimited && isAfter(ruleDate, aRule.getEndDate())) {
            aRule.setEndDate(ruleDate);
          }
        }
      }
    }
  }

  // Compute the max inherited date (note. end date must be computed before)
  private static void computeMaxEndDate(
      ArchiveUnit unit, AbstractComputedRules parentComputedRule) {
    if (parentComputedRule != null) {
      RuleType ruleType = parentComputedRule.getRuleType();
      ComputedInheritedRules cir = unit.getComputedInheritedRules();
      Management mgt = unit.getManagement();
      AbstractRules aRule = mgt == null ? null : mgt.getRules(ruleType);
      if (aRule == null) {
        AbstractComputedRules pcr = parentComputedRule.duplicate();
        pcr.setInheritanceOrigin(InheritanceOrigin.INHERITED);
        cir.setRules(pcr);
      } else {
        Boolean preventInheritance = aRule.getRuleInheritance().getPreventInheritance();
        AbstractComputedRules computedRule = cir.getRules(ruleType);
        LocalDate pcrMaxEndDate = parentComputedRule.getMaxEndDate();
        LocalDate endDate = aRule.getEndDate(); // null means "unlimited"
        if (BooleanUtils.isNotTrue(preventInheritance)
            && pcrMaxEndDate != null
            && endDate != null
            && pcrMaxEndDate.isAfter(endDate)) {
          computedRule.setMaxEndDate(pcrMaxEndDate);
          computedRule.setInheritanceOrigin(InheritanceOrigin.INHERITED);
          parentComputedRule.getRules().forEach(rule -> computedRule.getRules().add(rule));
        } else {
          computedRule.setMaxEndDate(endDate);
        }

        // FinalAction property is not inherited
        if (aRule instanceof FinalActionRule far) {
          ((FinalActionRule) computedRule).setFinalAction(far.getFinalAction());
        }
        // Classification properties are not inherited
        else if (aRule instanceof ClassificationRules cr) {
          setClassificationRules(cr, computedRule);
        }
      }
    }
  }

  private static void setClassificationRules(
      ClassificationRules cr, AbstractComputedRules computedRule) {
    ClassificationComputedRules ccr = (ClassificationComputedRules) computedRule;
    ccr.setNeedReassessingAuthorizations(list(cr.getNeedReassessingAuthorization()));
    ccr.setClassificationReassessingDates(list(cr.getClassificationReassessingDate()));
    ccr.setClassificationOwners(list(cr.getClassificationOwner()));
    ccr.setClassificationAudiences(list(cr.getClassificationAudience()));
    ccr.setClassificationLevels(list(cr.getClassificationLevel()));
  }

  private static <T> List<T> list(T value) {
    return value == null ? Collections.emptyList() : List.of(value);
  }

  private static void computeMaxEndDate(ArchiveUnit unit, HoldComputedRules parentComputedRule) {
    if (parentComputedRule != null) {
      ComputedInheritedRules cir = unit.getComputedInheritedRules();
      Management mgt = unit.getManagement();
      HoldRules aRule = mgt == null ? null : mgt.getHoldRules();
      if (aRule == null) {
        HoldComputedRules pcr = parentComputedRule.duplicate();
        pcr.setInheritanceOrigin(InheritanceOrigin.INHERITED);
        cir.setHoldComputedRules(pcr);
      } else {
        Boolean preventInheritance = aRule.getRuleInheritance().getPreventInheritance();
        HoldComputedRules computedRule = cir.getHoldComputedRules();
        LocalDate pcrMaxEndDate = parentComputedRule.getMaxEndDate();
        LocalDate endDate = aRule.getEndDate();
        Boolean rearrangement = aRule.getPreventRearrangement();
        if (BooleanUtils.isNotTrue(preventInheritance)
            && pcrMaxEndDate != null
            && endDate != null
            && pcrMaxEndDate.isAfter(endDate)) {
          computedRule.setMaxEndDate(pcrMaxEndDate);
          computedRule.setInheritanceOrigin(InheritanceOrigin.INHERITED);
          parentComputedRule.getRules().forEach(rule -> computedRule.getRules().add(rule));
          if (BooleanUtils.isTrue(rearrangement)) {
            computedRule.setPreventRearrangement(rearrangement);
          }
        } else {
          computedRule.setMaxEndDate(endDate);
          computedRule.setPreventRearrangement(rearrangement);
        }
      }
    }
  }

  private LocalDate getRuleDate(
      Long tenant, Map<String, RuleDb> ruleMap, Rule rule, RuleType ruleType) {

    RuleDb ruleDb =
        ruleMap.computeIfAbsent(rule.getRuleName(), id -> ruleService.getEntity(tenant, id));

    if (ruleDb.getStatus() == Status.INACTIVE) {
      throw new ManifestException(
          CHECK_MANAGEMENT_RULES_FAILED, String.format("Rule '%s' is inactive", ruleDb.getName()));
    }

    if (ruleDb.getType() != ruleType) {
      throw new ManifestException(
          CHECK_MANAGEMENT_RULES_FAILED,
          String.format("Rules '%s' and '%s' are not compatible", ruleType, ruleDb.getType()));
    }

    LocalDate startDate = rule.getStartDate();
    if (startDate == null) {
      return null;
    }

    if (startDate.isBefore(SipUtils.JAN_1000) || startDate.isAfter(SipUtils.DEC_6999)) {
      throw new ManifestException(
          CHECK_MANAGEMENT_RULES_FAILED,
          String.format(
              "Start date before '%s' and after '%s'", SipUtils.JAN_1000, SipUtils.DEC_6999));
    }

    String duration = ruleDb.getDuration().toLowerCase();
    return UNLIMITED.equals(duration)
        ? null
        : switch (ruleDb.getMeasurement()) {
          case DAY -> startDate.plusDays(Long.parseLong(duration));
          case MONTH -> startDate.plusMonths(Long.parseLong(duration));
          case YEAR -> startDate.plusYears(Long.parseLong(duration));
        };
  }
}
