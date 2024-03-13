/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.domain;

import java.util.Arrays;
import java.util.Optional;

public enum RuleType {
  AppraisalRule,
  AccessRule,
  StorageRule,
  DisseminationRule,
  ClassificationRule,
  ReuseRule,
  HoldRule;

  public static Optional<RuleType> fromName(String name) {
    return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(name)).findFirst();
  }
}
