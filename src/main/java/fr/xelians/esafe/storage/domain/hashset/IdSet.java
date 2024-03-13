/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.hashset;

import java.util.Iterator;
import java.util.List;

public interface IdSet extends Iterable<Long>, AutoCloseable {

  void add(long id);

  void remove(long id);

  long size();

  void close();

  Iterator<List<Long>> listIterator();
}
