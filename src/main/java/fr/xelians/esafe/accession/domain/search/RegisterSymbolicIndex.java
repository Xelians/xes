/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.domain.search;

/*
 * @author Emmanuel Deviller
 */
public class RegisterSymbolicIndex extends RegisterIndex {

  public static final String NAME = "registersymbolic";
  public static final String ALIAS = NAME + "_alias";

  public static final RegisterSymbolicIndex INSTANCE = new RegisterSymbolicIndex();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getAlias() {
    return ALIAS;
  }
}
