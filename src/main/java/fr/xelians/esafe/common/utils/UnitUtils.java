/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import static java.util.stream.Collectors.groupingBy;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.BinaryVersion;
import fr.xelians.esafe.archive.domain.unit.rules.inherited.*;
import fr.xelians.esafe.common.exception.technical.InternalException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public final class UnitUtils {
  private UnitUtils() {}

  public static BinaryQualifier getBinaryQualifier(String binaryVersion) {
    if (StringUtils.isBlank(binaryVersion)) {
      return BinaryQualifier.BinaryMaster;
    }
    String[] tks = StringUtils.split(binaryVersion, '_');
    return BinaryQualifier.valueOf(tks[0]);
  }

  public static BinaryVersion getBinaryVersion(String binaryVersion) {
    if (StringUtils.isBlank(binaryVersion)) {
      return new BinaryVersion(BinaryQualifier.BinaryMaster, null);
    }
    String[] tks = StringUtils.split(binaryVersion, '_');
    BinaryQualifier qualifier = BinaryQualifier.valueOf(tks[0]);
    return new BinaryVersion(qualifier, tks.length == 1 ? null : Integer.valueOf(tks[1]));
  }

  public static void checkVersion(ArchiveUnit indexedArchiveUnit, ArchiveUnit storedArchiveUnit) {
    Long archiveUnitId = indexedArchiveUnit.getId();

    // Stored archive must already exist
    if (storedArchiveUnit == null) {
      throw new InternalException(
          String.format("Archive unit id '%s' was not found on storage offer", archiveUnitId));
    }

    // Optimistic lock on auto version
    int indexedAv = indexedArchiveUnit.getAutoversion();
    int storedAv = storedArchiveUnit.getAutoversion();
    if (indexedAv != storedAv) {
      throw new InternalException(
          String.format(
              "Archive unit id '%s' indexed version '%s' is different from stored version '%s'",
              archiveUnitId, indexedAv, storedAv));
    }
  }

  public static Map<Long, ArchiveUnit> mapById(List<ArchiveUnit> archiveUnits) {
    return archiveUnits.stream().collect(Collectors.toMap(ArchiveUnit::getId, Function.identity()));
  }

  public static Map<Long, List<ArchiveUnit>> groupByOpId(List<ArchiveUnit> archiveUnits) {
    return archiveUnits.stream().collect(groupingBy(ArchiveUnit::getOperationId));
  }
}
