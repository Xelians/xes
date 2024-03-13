/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.task;

import java.util.concurrent.FutureTask;
import lombok.Getter;

@Getter
public class FutureOperationTask<T> extends FutureTask<T> {

  private final OperationTask<T> operationTask;

  public FutureOperationTask(OperationTask<T> callable) {
    super(callable);
    this.operationTask = callable;
  }
}
