/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

// A leaf operator is a terminal operator that cannot contain other operator
// For example a match operator :  { "$match": { "Title": "Porte de Bagnolet", "DocType": "Contrat"
// } }
public interface LeafQueryOperator<T> extends Operator {

  T create();
}
