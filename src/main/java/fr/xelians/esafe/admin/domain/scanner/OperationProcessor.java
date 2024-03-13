/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner;

import fr.xelians.esafe.operation.entity.OperationSe;
import java.io.IOException;

public interface OperationProcessor {

  void process(OperationSe operation) throws IOException;

  void finish() throws IOException;
}
