/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.task;

import java.util.concurrent.Callable;

public interface CleanableTask<T> extends Callable<T> {

  void clean();
}
