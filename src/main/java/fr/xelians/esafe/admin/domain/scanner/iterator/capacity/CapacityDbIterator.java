/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.capacity;

import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import java.util.List;

public class CapacityDbIterator extends DbIterator {

  public CapacityDbIterator(Long tenant, Long idMax, OperationService operationService) {
    super(tenant, idMax, operationService);
  }

  // TODO the findOperation list the operations with all action
  /// However we don't need the ADDDBO and DELDBO actions
  // We have to filter it
  @Override
  public List<OperationDb> findOperations(Long tenant, Long idMin, Long idMax) {
    return operationService.findForCapacity(tenant, idMin, idMax);
  }
}
