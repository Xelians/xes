/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.parser;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMap;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.FieldUtils;
import fr.xelians.esafe.search.domain.dsl.operator.*;
import fr.xelians.esafe.search.domain.field.Field;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

public abstract class DslParser<T> {

  protected static final String CREATION_FAILED = "Failed to create query";

  protected static final int FROM = 0;
  protected static final int SIZE = 1;
  protected static final int LIMIT_MAX = 10_000;
  protected static final String OFFSET_FIELD = "$offset";
  protected static final String LIMIT_FIELD = "$limit";
  protected static final String FIELDS_FIELD = "$fields";

  @Getter protected final Searchable searchable;

  protected DslParser(Searchable searchable) {
    this.searchable = searchable;
  }

  public abstract OntologyMapper getOntologyMapper();

  @SuppressWarnings("unchecked")
  protected T create(Operator operator) {
    if (operator instanceof BooleanQueryOperator<?> booleanQueryOperator) {
      List<T> queries = booleanQueryOperator.getOperators().stream().map(this::create).toList();
      return ((BooleanQueryOperator<T>) booleanQueryOperator).create(queries);

    } else if (operator instanceof LeafQueryOperator<?> leafQueryOperator) {
      return (T) leafQueryOperator.create();
    }
    throw new BadRequestException(
        CREATION_FAILED, String.format("Unknown operator: %s", operator.name()));
  }

  public Field getQueryField(String docType, String fieldName) {
    Field field = getNamedField(docType, fieldName, FieldContext.QUERY).field();
    if (field == null) {
      throwBadRequestException(String.format("Field '%s' does not exist in mapping", fieldName));
    }
    return field;
  }

  public String getProjectionFieldName(String docType, String fieldName) {
    return getNamedField(docType, fieldName, FieldContext.PROJECTION).fieldName();
  }

  protected NamedField getNamedField(String docType, String fieldName, FieldContext fieldContext) {

    if (searchable.isSpecialField(fieldName)) {
      return getSpecialNamedField(fieldName, fieldContext);
    }

    Field field = searchable.getAliasField(fieldName, fieldContext);
    if (field != null) {
      return new NamedField(fieldName, field);
    }

    // Do not accept _ and others weirds characters
    if (FieldUtils.isNotAlphaNumeric(fieldName)) {
      throwBadRequestException(
          String.format("Field '%s' must only contain alpha numeric characters", fieldName));
    }

    if (searchable.isStdField(fieldName)) {
      return new NamedField(fieldName, searchable.getField(fieldName));
    }

    if (searchable.isExtField(fieldName)) {
      return createExtNameField(fieldName, searchable.getField(fieldName));
    }

    OntologyMapper ontologyMapper = getOntologyMapper();
    if (ontologyMapper == null) {
      return new NamedField(fieldName, null);
    }

    // If ontologyMapper is not null and docType is null then the default mapper is selected
    OntologyMap ontologyMap = ontologyMapper.getOntologyMap(docType);
    if (ontologyMap == null) {
      throwBadRequestException(String.format("Ontology does not exist for #type '%s'", docType));
    }

    String mappedName = ontologyMap.get(fieldName);
    if (StringUtils.isBlank(mappedName)) {
      return createExtNameField(fieldName, null);
    }

    return createExtNameField(fieldName, searchable.getField(mappedName));
  }

  private NamedField createExtNameField(String fieldName, Field field) {
    return new NamedField("_extents." + fieldName, field);
  }

  protected NamedField getSpecialNamedField(String fieldName, FieldContext fieldContext) {
    String name;
    int i = fieldName.indexOf('.');
    if (i > 0) {
      String lastName = fieldName.substring(i);
      if (FieldUtils.isNotAlphaNumeric(lastName)) {
        throwBadRequestException(
            String.format("Field '%s' must only contain alpha numeric characters", fieldName));
      }
      name = searchable.getSpecialFieldName(fieldName.substring(0, i), fieldContext) + lastName;
    } else {
      name = searchable.getSpecialFieldName(fieldName, fieldContext);
    }

    if (StringUtils.isBlank(name)) {
      throwBadRequestException(String.format("Field '%s' is not a valid special field", fieldName));
    }
    return new NamedField(name, searchable.getField(name));
  }

  public NamedField getUpdateNameField(String docType, String fieldName) {
    return getNamedField(docType, fieldName, FieldContext.UPDATE);
  }

  protected abstract Operator createOperator(
      SearchContext searchContext, String operator, JsonNode node);

  public List<Operator> createOperators(SearchContext searchContext, JsonNode node) {
    List<Operator> operators = new ArrayList<>();

    if (node.isObject()) {
      traverse(searchContext, node, operators);
    } else if (node.isArray()) {
      node.forEach(e -> traverse(searchContext, e, operators));
    } else {
      throw new InternalException(
          "Failed to create operators",
          String.format("Node '%s' must be an object or an array", node.asText()));
    }
    return operators;
  }

  protected void traverse(SearchContext searchContext, JsonNode node, List<Operator> operators) {
    node.fields()
        .forEachRemaining(
            entry ->
                operators.add(createOperator(searchContext, entry.getKey(), entry.getValue())));
  }

  protected int[] createLimits(JsonNode node) {
    int[] limits = {0, 100};
    if (node != null) {
      JsonNode offsetNode = node.get(OFFSET_FIELD);
      if (offsetNode != null) {
        limits[FROM] = offsetNode.asInt();
      }
      JsonNode limitNode = node.get(LIMIT_FIELD);
      if (limitNode != null) {
        limits[SIZE] = Math.min(LIMIT_MAX, limitNode.asInt());
      }
    }
    return limits;
  }

  protected List<String> createProjectionFields(SearchContext searchContext, JsonNode node) {
    Set<String> fields = new HashSet<>();
    if (node != null) {
      if (!node.isObject()) {
        throwBadRequestException("Failed to process $projection - not an object");
      }
      JsonNode fieldsNode = node.get(FIELDS_FIELD);
      if (fieldsNode != null) {
        if (!fieldsNode.isObject()) {
          throwBadRequestException("Failed to process projection $fields - not an object");
        }
        fieldsNode
            .fields()
            .forEachRemaining(
                entry -> {
                  if (entry.getValue().asInt() == 1) {
                    String fieldName =
                        getProjectionFieldName(searchContext.getDocType(), entry.getKey());
                    fields.add(fieldName);
                  }
                });
      }
    }
    return new ArrayList<>(fields);
  }

  protected static void throwBadRequestException(String message) {
    throw new BadRequestException(CREATION_FAILED, message);
  }
}
