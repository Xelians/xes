/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules;

/*
 * @author Emmanuel Deviller
 */
public interface FinalActionRule {

  String getFinalAction();

  void setFinalAction(String finalAction);
}
