/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

// A leaf operator is a terminal operator that cannot contain other operator
// For example a match operator :  { "$match": { "Title": "Porte de Bagnolet", "DocType": "Contrat"
// } }
/*
 * @author Emmanuel Deviller
 */
public interface LeafQueryOperator<T> extends Operator {

  T create();
}
