/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.pack;

import java.io.IOException;
import java.nio.file.Path;

public interface Packer extends AutoCloseable {

  long[] write(Path srcPath) throws IOException;
}
