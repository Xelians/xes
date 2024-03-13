/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.entity.RuleDb;
import java.nio.file.Path;
import java.util.Map;

public record Context(
    OperationDb operationDb,
    TenantDb tenantDb,
    AccessContractDb accessContractDb,
    OntologyMapper ontologyMapper,
    Map<String, RuleDb> ruleMap,
    Path path) {}
