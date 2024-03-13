/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.task;

import fr.xelians.esafe.operation.entity.OperationDb;

public interface OperationTask<T> extends CleanableTask<T> {

  OperationDb getOperation();

  void setActive(boolean b);

  boolean isActive();

  boolean isExclusive();
}
