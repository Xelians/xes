/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.update;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitParser;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.search.domain.dsl.operator.eql.Patch;
import fr.xelians.esafe.search.domain.dsl.operator.eql.Set;
import fr.xelians.esafe.search.domain.dsl.operator.eql.Unset;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import java.util.Iterator;
import java.util.Map;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
public class UpdateParser extends ArchiveUnitParser {

  public UpdateParser(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    super(tenant, accessContractDb, ontologyMapper);
  }

  public static UpdateParser create(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContractDb, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(ontologyMapper, ONTOLOGY_MAPPER_MUST_BE_NOT_NULL);

    return new UpdateParser(tenant, accessContractDb, ontologyMapper);
  }

  public UpdateRequest createRequest(UpdateQuery updateQuery) {
    if (isEmpty(updateQuery.queryNode())) {
      throw new BadRequestException(CREATION_FAILED, QUERY_IS_EMPTY_OR_NOT_DEFINED);
    }

    if (accessContractDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format(ACCESS_CONTRACT_IS_INACTIVE, accessContractDb.getIdentifier()));
    }

    if (Utils.isFalse(accessContractDb.getWritingPermission())) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format(
              "Access Contrat '%s' does not allow modification", accessContractDb.getIdentifier()));
    }

    // The context of this query
    SearchContext searchContext = new SearchContext(updateQuery.type());

    return new UpdateRequest(
        doCreateActionSearchRequest(searchContext, updateQuery),
        doCreateJsonPatch(searchContext, updateQuery.actionNode()));
  }

  private JsonNode doCreateJsonPatch(SearchContext searchContext, JsonNode actionNode) {

    if (!actionNode.isArray() || actionNode.isEmpty()) {
      throwBadRequestException("Action must be a non empty array");
    }

    JsonPatchBuilder jsonPatchBuilder = new JsonPatchBuilder();

    for (JsonNode node : actionNode) {
      for (Iterator<Map.Entry<String, JsonNode>> ite = node.fields(); ite.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = ite.next();
        String key = entry.getKey();
        Patch operator =
            switch (key) {
              case "$set" -> new Set(this, searchContext, entry.getValue());
              case "$unset" -> new Unset(this, searchContext, entry.getValue());
              default -> throw new BadRequestException(
                  CREATION_FAILED, String.format("Unknown update operator: '%s'", key));
            };
        jsonPatchBuilder.op(operator.getJsonPatchOp());
      }
    }

    JsonNode jsonPatch = jsonPatchBuilder.build();
    if (jsonPatch.isEmpty()) {
      throwBadRequestException("Update action patch is empty");
    }

    return jsonPatch;
  }
}
