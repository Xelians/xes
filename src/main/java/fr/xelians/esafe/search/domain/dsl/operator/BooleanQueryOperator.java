/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import java.util.List;

/**
 * Interface représentant un opérateur de combinaison d'opérateur.
 *
 * @extends Operator
 * @author Emmanuel Deviller
 */
public interface BooleanQueryOperator<T> extends Operator {

  /**
   * Renvoie la liste des opérateurs associés à cet opérateur de combinaison.
   *
   * @return La liste des opérateurs associés à cet opérateur de combinaison.
   */
  List<Operator> getOperators();

  /**
   * Crée la requête combinée à partir de la liste des requêtes fournies.
   *
   * @param queries La liste des requêtes à combiner.
   * @return La requête combinée créée à partir de la liste de requêtes fournies.
   */
  T create(List<T> queries);
}
