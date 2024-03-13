/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.hashset;

import fr.xelians.esafe.common.utils.ListIterator;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.set.ChronicleSet;
import net.openhft.chronicle.set.ChronicleSetBuilder;

@Slf4j
public class ChronicleIdSet implements IdSet {

  public static final int LIST_SIZE = 50_000;

  private final ChronicleSet<Long> ids;

  public ChronicleIdSet(long capacity) throws IOException {
    try {
      // Note. 1 billion of long takes 20Gb of off-heap memory
      ids = ChronicleSetBuilder.of(Long.class).name("id_set").entries(capacity).create();
    } catch (Throwable throwable) {
      throw new IOException(
          "Failed to create ChronicleMap - See https://chronicle.software/chronicle-support-java-17/",
          throwable);
    }
  }

  @Override
  public void add(long id) {
    ids.add(id);
  }

  @Override
  public void remove(long id) {
    ids.remove(id);
  }

  @Override
  public long size() {
    return ids.size();
  }

  // Note. This iterator is not thread safe
  @Override
  public Iterator<Long> iterator() {
    return ids.iterator();
  }

  @Override
  public Iterator<List<Long>> listIterator() {
    return ListIterator.iterator(this, LIST_SIZE);
  }

  @Override
  public void close() {
    if (ids != null) {
      ids.close();
    }
  }
}
