/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner;

import com.machinezoo.noexception.Exceptions;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public abstract class AllLbkIterator extends LbkIterator {

  private List<Long> lbkIds;
  private int idx = -1;

  protected AllLbkIterator(TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  protected boolean hastNextOperation() {
    try {
      while (lbkReader == null || (line = lbkReader.readLine()) == null) {
        closeLbkReader();

        if (lbkIds == null) {
          initLbkIds();
        }

        // Next Oplog
        idx++;
        if (idx < lbkIds.size()) {
          lbkId = lbkIds.get(idx);
          createLbkReader();
          continue;
        }

        // No more oplog
        line = null;
        return false;
      }

      // Next line
      return true;

    } catch (IOException ex) {
      throw Exceptions.sneak().handle(ex);
    }
  }

  private void initLbkIds() throws IOException {
    long sn = storageService.getSecureNumber(tenantDb.getId());
    lbkIds =
        storageService.findLbkIds(tenantDb, offers).stream().filter(n -> n <= sn).sorted().toList();
  }
}
