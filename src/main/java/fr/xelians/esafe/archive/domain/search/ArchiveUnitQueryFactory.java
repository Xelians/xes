/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationQuery;
import fr.xelians.esafe.archive.domain.search.export.DipExportType;
import fr.xelians.esafe.archive.domain.search.export.ExportQuery;
import fr.xelians.esafe.archive.domain.search.probativevalue.ProbativeValueQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRuleQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;

public class ArchiveUnitQueryFactory {

  protected static final String CREATION_FAILED = "Failed to create query";

  protected static final ObjectMapper readMapper;
  protected static final ObjectReader objectReader;

  static {
    readMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    readMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    readMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    objectReader = readMapper.reader();
  }

  private ArchiveUnitQueryFactory() {}

  public static SearchQuery createSearchQuery(String dsl) {
    try {
      return objectReader.readValue(dsl, SearchQuery.class);
    } catch (IOException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Failed to parse json search query: %s", ex.getMessage()));
    }
  }

  public static UpdateQuery createUpdateQuery(String dsl) {
    try {
      return objectReader.readValue(dsl, UpdateQuery.class);
    } catch (IOException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Failed to parse json update query: %s", ex.getMessage()));
    }
  }

  public static EliminationQuery createEliminationQuery(String dsl) {
    try {
      return objectReader.readValue(dsl, EliminationQuery.class);
    } catch (IOException ex) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format("Failed to parse json elimination query: %s", ex.getMessage()));
    }
  }

  public static UpdateRuleQuery createUpdateRuleQuery(String dsl) {
    try {
      UpdateRuleQuery updateRuleQuery = objectReader.readValue(dsl, UpdateRuleQuery.class);

      if (updateRuleQuery == null) {
        throw new BadRequestException(
            String.format("The update rules type query %s is empty or ill-formed", dsl));
      }

      return updateRuleQuery;
    } catch (IOException ex) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format("Failed to parse json update rules query: %s", ex.getMessage()));
    }
  }

  public static ExportQuery createExportQuery(String dsl) {
    try {
      ExportQuery exportQuery = objectReader.readValue(dsl, ExportQuery.class);

      if (exportQuery == null) {
        throw new BadRequestException(
            String.format("The dip export type query %s is empty or ill-formed", dsl));
      }

      if (exportQuery.dipExportType() == DipExportType.MINIMAL) {
        throw new BadRequestException(
            CREATION_FAILED,
            "The minimal dip export type is not currently supported. You have to use the full export type instead.");
      }

      if (exportQuery.dipRequestParameters() == null) {
        throw new BadRequestException(
            CREATION_FAILED,
            "The mandatory dip export request parameters are not defined in the request.");
      }

      if (StringUtils.isBlank(exportQuery.dipRequestParameters().archivalAgencyIdentifier())) {
        throw new BadRequestException(
            CREATION_FAILED,
            "The mandatory originating agency identifier parameter is not defined in the dip export request.");
      }

      if (StringUtils.isBlank(exportQuery.dipRequestParameters().messageRequestIdentifier())) {
        throw new BadRequestException(
            CREATION_FAILED,
            "The mandatory message request identifier parameter is not defined in the dip export request.");
      }

      if (exportQuery.searchQuery() == null) {
        throw new BadRequestException(
            CREATION_FAILED, "The mandatory dsl request is not defined in the dip export request.");
      }

      return exportQuery;
    } catch (IOException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Failed to parse json export query: %s", ex.getMessage()));
    }
  }

  public static ProbativeValueQuery createProbativeValueQuery(String dsl) {
    try {
      ProbativeValueQuery probativeValueQuery =
          objectReader.readValue(dsl, ProbativeValueQuery.class);

      if (probativeValueQuery == null) {
        throw new BadRequestException(
            String.format("The probative value query %s is empty or ill-formed", dsl));
      }

      if (probativeValueQuery.searchQuery() == null) {
        throw new BadRequestException(
            CREATION_FAILED,
            "The mandatory dsl query is not defined in the probative value query.");
      }

      return probativeValueQuery;
    } catch (IOException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Failed to parse json export query: %s", ex.getMessage()));
    }
  }
}
