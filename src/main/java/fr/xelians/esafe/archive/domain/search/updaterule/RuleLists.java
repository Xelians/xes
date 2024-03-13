/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import fr.xelians.esafe.archive.domain.unit.rules.management.AbstractRules;
import fr.xelians.esafe.referential.domain.RuleType;
import java.util.List;
import java.util.Map;

public record RuleLists(
    List<RuleTypeName> deleteRules,
    Map<RuleType, UpdateRules> updateRules,
    List<AbstractRules> addRules) {}
