/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.constraint.JsonSize;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * $roots (des unités d'archives) représente les racines à partir desquelles la requête de recherche
 * est exprimée.
 *
 * <p>$depth spécifie la profondeur maximale de recherche dans l'arborescence, soit à partir des
 * $roots, soit à partir de la racine absolue, dans laquelle la recherche doit s'exécuter. La valeur
 * de $depth doit être un entier supérieur ou égal à zéro.
 *
 * <p>Si $depth = 0, la recherche est réalisée uniquement sur les unités précisées dans $roots. Si
 * $roots ne spécifie pas de racines, $depth ne peut pas être nul.
 *
 * <p>Si $depth > 0 && < MAX_DEPTH, la recherche est réalisée sur les unités enfants jusqu'à la
 * profondeur spécifiée sans prendre en compte les unités $roots elles-mêmes.
 *
 * <p>Si $depth > MAX_DEPTH, la recherche est réalisée sur toutes les unités d'archive et sans
 * prendre en compte les unités $roots elles-mêmes.
 *
 * <p>La valeur MAX_DEPTH est égale à 10 par défaut. Elle est paramétrable au niveau de l'instance
 * de l'application.
 *
 * <p>Notes. Dans VITAM, la limite de profondeur n'existe pas. Dans ESAFE, le paramètre $depth peut
 * être spécifiée même si $roots ne précise pas de racines.
 *
 * <p>$filter permet de trier ou de limiter le nombre de résultats retournés
 *
 * <p>$projections précise l'ensemble des champs devant être retournés
 *
 * <p>$facets représente n tableau de requêtes d'agrégation
 */
@Builder
public record SearchQuery(
    @JsonProperty("$roots") @Size(max = 1024) List<Long> roots,
    @JsonProperty("$type") @Size(max = 1024) String type,
    @JsonProperty("$query") @JsonSize JsonNode queryNode,
    @JsonProperty("$threshold") Long threshold,
    @JsonProperty("$filter") @JsonSize JsonNode filterNode,
    @JsonProperty("$projection") @JsonSize JsonNode projectionNode,
    @JsonProperty("$facets") @JsonSize JsonNode facetsNode) {}
