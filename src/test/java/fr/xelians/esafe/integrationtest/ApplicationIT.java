/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ApplicationIT extends BaseIT {

  @Test
  void contextLoads() {
    String s1 = "Hello Spring World";
    String s2 = "hELLO sPRING wORLD";
    assertEquals(s1.toLowerCase(), s2.toLowerCase());
  }
}
