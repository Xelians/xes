/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
