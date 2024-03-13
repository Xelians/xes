/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.service;

import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.mget.MultiGetOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.logbook.domain.search.LogbookIndex;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.search.service.SearchEngineService;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleConverterService {

  private final SearchEngineService searchEngineService;

  public JsonNode convertUnitLfc(Long tenant, JsonNode srcNode) throws IOException {

    ArrayNode anode = JsonNodeFactory.instance.arrayNode();
    JsonNode lfcNodes = srcNode.get("_lifeCycles");
    Map<Long, OperationSe> operations = getUnitOperationMap(tenant, srcNode, lfcNodes);
    String unitId = srcNode.get("_unitId").asText();
    String sp = srcNode.get("_sp").asText();
    addFirstUnitLfc(anode, sp, unitId, srcNode, operations);
    if (lfcNodes != null && !lfcNodes.isEmpty()) {
      addNextUnitLfc(anode, sp, unitId, lfcNodes, operations);
    }

    ObjectNode dstNode = JsonNodeFactory.instance.objectNode();
    // dstNode.put("httpCode", 200);
    dstNode.set("$hits", createHit(anode.size()));
    dstNode.set("$context", createContext());
    dstNode.set("$results", anode);
    return dstNode;
  }

  public JsonNode convertObjectLfc(Long tenant, JsonNode srcNode) throws IOException {

    ArrayNode anode = JsonNodeFactory.instance.arrayNode();
    JsonNode bdoNodes = srcNode.get("BinaryDataObjects");
    if (bdoNodes != null && !bdoNodes.isEmpty()) {
      Map<Long, OperationSe> operations = getObjectOperationMap(tenant, bdoNodes);
      String sp = srcNode.get("_sp").asText();
      addObjectLfc(anode, sp, bdoNodes, operations);
    }

    ObjectNode dstNode = JsonNodeFactory.instance.objectNode();
    // dstNode.put("httpCode", 200);
    dstNode.set("$hits", createHit(anode.size()));
    dstNode.set("$context", createContext());
    dstNode.set("$results", anode);
    return dstNode;
  }

  private JsonNode createHit(int n) {
    ObjectNode hitNode = JsonNodeFactory.instance.objectNode();
    hitNode.put("total", n);
    hitNode.put("size", n);
    hitNode.put("offset", 0);
    hitNode.put("limit", 10000); // Infinite!
    return hitNode;
  }

  private JsonNode createContext() {
    ObjectNode contextNode = JsonNodeFactory.instance.objectNode();
    contextNode.set("$projection", JsonNodeFactory.instance.objectNode());
    return contextNode;
  }

  private Map<Long, OperationSe> getObjectOperationMap(Long tenant, JsonNode bdoNodes)
      throws IOException {
    Set<String> opis = new HashSet<>();
    for (JsonNode bdoNode : bdoNodes) {
      opis.add(bdoNode.get("_opi").asText());
    }
    return getLogbookOperationsByIds(tenant, opis);
  }

  private void addObjectLfc(
      ArrayNode anode, String sp, JsonNode bdoNodes, Map<Long, OperationSe> operations) {
    for (JsonNode bdoNode : bdoNodes) {
      Long opi = bdoNode.get("_opi").asLong();
      OperationSe operation = operations.get(opi);
      String id = nonNull(bdoNode.get("_binaryId"));
      String detail =
          bdoNode.get("DigestAlgorithm").asText() + ":" + bdoNode.get("MessageDigest").asText();
      anode.add(createLfc(operation, sp, id, detail));
    }
  }

  private Map<Long, OperationSe> getUnitOperationMap(
      Long tenant, JsonNode srcNode, JsonNode lfcNodes) throws IOException {
    Set<String> opis = new HashSet<>();
    opis.add(srcNode.get("_opi").asText());
    if (lfcNodes != null) {
      for (JsonNode lfcNode : lfcNodes) {
        opis.add(lfcNode.get("_opi").asText());
      }
    }
    return getLogbookOperationsByIds(tenant, opis);
  }

  private Map<Long, OperationSe> getLogbookOperationsByIds(Long tenant, Set<String> ids)
      throws IOException {
    if (ids.isEmpty()) return Collections.emptyMap();

    List<MultiGetOperation> mgetList =
        ids.stream().map(id -> new MultiGetOperation.Builder().id(id).build()).toList();
    MgetRequest request =
        new MgetRequest.Builder().index(LogbookIndex.ALIAS).docs(mgetList).build();
    return searchEngineService
        .getMultiById(request, OperationSe.class)
        .filter(ope -> tenantFilter(tenant, ope))
        .collect(Collectors.toMap(OperationSe::getId, Function.identity()));
  }

  private boolean tenantFilter(Long tenant, OperationSe operation) {
    if (operation.getTenant().equals(tenant)) return true;
    throw new InternalException(
        String.format(
            "Operation id '%s' has bad tenant '%s' instead of '%s'",
            operation.getId(), operation.getTenant(), tenant));
  }

  private void addFirstUnitLfc(
      ArrayNode anode, String sp, String id, JsonNode srcNode, Map<Long, OperationSe> operations) {
    Long opi = srcNode.get("_opi").asLong();
    OperationSe operation = operations.get(opi);
    anode.add(createLfc(operation, sp, id, ""));
  }

  private void addNextUnitLfc(
      ArrayNode anode, String sp, String id, JsonNode lfcNodes, Map<Long, OperationSe> operations) {
    for (JsonNode lfcNode : lfcNodes) {
      Long opi = lfcNode.get("_opi").asLong();
      OperationSe operation = operations.get(opi);
      String detail = nonNull(lfcNode.get("_patch"));
      anode.add(createLfc(operation, sp, id, detail));
    }
  }

  private String nonNull(JsonNode node) {
    return node == null ? "" : node.asText();
  }

  private JsonNode createLfc(OperationSe operation, String sp, String id, String detail) {

    ObjectNode lfcNode = JsonNodeFactory.instance.objectNode();
    lfcNode.put("eventIdentifier", operation.getId());
    lfcNode.put("eventType", operation.getType().toString());
    lfcNode.put("evDateTime", operation.getCreated().toString());
    lfcNode.put("eventIdentifierProcess", "DefaultWorkflow");
    lfcNode.put("eventTypeProcess", operation.getType().toString());
    lfcNode.put("outcome", getOutcome(operation));
    lfcNode.put("eventOutcomeDetail", detail);
    lfcNode.put("eventOutcomeDetailMessage", getMessage(operation));
    lfcNode.put("agentIdentifier", operation.getUserIdentifier());
    lfcNode.put("agentIdentifierApplication", operation.getApplicationId());
    lfcNode.put("agentIdentifierApplicationSession", "");
    lfcNode.put("eventIdentifierRequest", "");
    lfcNode.put("agentIdentifierSubmission", "");
    lfcNode.put("agentIdentifierOriginating", sp);
    lfcNode.put("objectIdentifier", id);
    lfcNode.put("objectIdentifierRequest", operation.getObjectInfo());
    lfcNode.put(
        "objectIdentifierIncome", operation.getObjectIdentifier()); // MessageIdentifier_in_SEDA
    return lfcNode;
  }

  private String getOutcome(OperationSe operation) {
    return operation.getOutcome().isEmpty() ? "OK" : operation.getOutcome();
  }

  private String getMessage(OperationSe operation) {
    return operation.getMessage().isEmpty()
        ? "Operation completed with success"
        : operation.getMessage();
  }
}
