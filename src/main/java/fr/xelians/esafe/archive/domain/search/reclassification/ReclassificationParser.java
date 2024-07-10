/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.reclassification;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitParser;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.search.domain.dsl.operator.eql.Add;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import io.jsonwebtoken.lang.Assert;
import java.util.Iterator;
import java.util.Map;

public class ReclassificationParser extends ArchiveUnitParser {

  public ReclassificationParser(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    super(tenant, accessContractDb, ontologyMapper);
  }

  public static ReclassificationParser create(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContractDb, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(ontologyMapper, ONTOLOGY_MAPPER_MUST_BE_NOT_NULL);

    return new ReclassificationParser(tenant, accessContractDb, ontologyMapper);
  }

  public ReclassificationRequest createRequest(UpdateQuery reclassificationQuery) {
    if (isEmpty(reclassificationQuery.queryNode())) {
      throw new BadRequestException(CREATION_FAILED, QUERY_IS_EMPTY_OR_NOT_DEFINED);
    }

    if (accessContractDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format("Access Contract '%s' is inactive", accessContractDb.getIdentifier()));
    }

    if (Utils.isFalse(accessContractDb.getWritingPermission())) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format(
              "Access Contract '%s' does not allow modification",
              accessContractDb.getIdentifier()));
    }

    // The context of this query
    SearchContext searchContext = new SearchContext(reclassificationQuery.type());

    return new ReclassificationRequest(
        doCreateActionSearchRequest(searchContext, reclassificationQuery),
        doCreateReclassification(searchContext, reclassificationQuery.actionNode()));
  }

  private Long doCreateReclassification(SearchContext searchContext, JsonNode actionNode) {

    if (!actionNode.isArray() || actionNode.isEmpty()) {
      throwBadRequestException("Action must be a non empty array");
    }

    Long unitUp = null;

    for (JsonNode node : actionNode) {
      if (unitUp == null) {
        for (Iterator<Map.Entry<String, JsonNode>> ite = node.fields(); ite.hasNext(); ) {
          Map.Entry<String, JsonNode> entry = ite.next();
          String key = entry.getKey();
          if (!"$add".equals(key)) {
            throw new BadRequestException(
                CREATION_FAILED, String.format("Unknown reclassification operator: '%s'", key));
          }
          unitUp = new Add(this, searchContext, entry.getValue()).getUnitUp();
        }
      } else {
        throwBadRequestException("Reclassification unit is already defined");
      }
    }

    if (unitUp == null) {
      throwBadRequestException("Reclassification unit is not defined");
    }

    return unitUp;
  }
}
