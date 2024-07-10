/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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
