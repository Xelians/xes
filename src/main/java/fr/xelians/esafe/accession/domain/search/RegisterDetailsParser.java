/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.domain.search;

import fr.xelians.esafe.search.domain.dsl.parser.eql.SimpleEqlParser;
import io.jsonwebtoken.lang.Assert;

public class RegisterDetailsParser extends SimpleEqlParser {

  private RegisterDetailsParser(Long tenant) {
    super(RegisterDetailsIndex.INSTANCE, tenant);
  }

  public static RegisterDetailsParser create(Long tenant) {
    Assert.notNull(tenant, "Tenant must be not null");
    return new RegisterDetailsParser(tenant);
  }
}
