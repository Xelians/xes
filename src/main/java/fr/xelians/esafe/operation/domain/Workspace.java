/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.domain;

import fr.xelians.esafe.common.constant.Env;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Workspace {

  private Workspace() {
    // Do nothing
  }

  public static Path getPath(OperationDb operation) {
    return getPath(operation.getId());
  }

  public static Path getPath(Long operationId) {
    return Env.OPERATION_PATH.resolve(operationId.toString());
  }

  public static Path getPath(OperationDb operation, String suffix) {
    return getPath(operation.getId()).resolve(suffix);
  }

  public static Path createTempFile(OperationDb operation) throws IOException {
    Path ws = getPath(operation.getId());
    if (Files.notExists(ws)) Files.createDirectories(ws);
    return Files.createTempFile(ws, null, null);
  }
}
