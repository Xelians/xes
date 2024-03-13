/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.copy;

import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import java.util.List;

public class CopyDbIterator extends DbIterator {

  public CopyDbIterator(Long tenant, long idMax, OperationService operationService) {
    super(tenant, idMax, operationService);
  }

  @Override
  public List<OperationDb> findOperations(Long tenant, Long idMin, Long idMax) {
    return operationService.findForCopy(tenant, idMin, idMax);
  }
}
