/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

/**
 * binOperator : $and | $or | $not
 *
 * <p>leafOperator : $match | $match_all | $match_phrase | $match_phrase_prefix | $eq | $ne | $lt |
 * $lte | $gt | $gte | $range | $in | $nin | $exists | $search | $wildcard | $regex |
 *
 * <p>field : String
 *
 * <p>value : String | { parameter }
 *
 * <p>parameter : [ { "field" : value , ... }, ... ] |
 *
 * <p>operator : [ { "binOperator" : operator }, ... ] | { "leafOperator" : parameter }
 *
 * <p>query : operator, { $depth : Long }
 *
 * <p>#id :
 */
public interface Operator {

  String name();
}
