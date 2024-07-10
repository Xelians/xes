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

public abstract class OneLbkIterator extends LbkIterator {

  protected OneLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService, long logId) {
    super(tenantDb, offers, storageService);
    this.lbkId = logId;
  }

  protected boolean hastNextOperation() {
    try {
      if (lbkReader == null) {
        createLbkReader();
      }

      // Read next line
      line = lbkReader.readLine();

      if (line == null) {
        closeLbkReader();
        return false;
      }

      return true;

    } catch (IOException ex) {
      throw Exceptions.sneak().handle(ex);
    }
  }
}
