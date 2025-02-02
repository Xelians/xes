/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner;

import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public abstract class DbIterator implements Iterator<OperationDb> {

  protected final OperationService operationService;
  protected final Long tenant;
  protected final Long idMax;

  private Iterator<OperationDb> iterator;
  private Long idMin = -1L;

  protected DbIterator(Long tenant, Long idMax, OperationService operationService) {
    this.tenant = tenant;
    this.idMax = idMax;
    this.operationService = operationService;
  }

  @Override
  public boolean hasNext() {
    if (iterator == null || !iterator.hasNext()) {
      List<OperationDb> operations = findOperations(tenant, idMin, idMax);
      log.info(
          "tenant {} - DB - idMin: {} - idMax: {} - operations: {}",
          tenant,
          idMin,
          idMax,
          operations.size());
      if (operations.isEmpty()) {
        return false;
      }
      iterator = operations.iterator();
      idMin = operations.getLast().getId();
    }

    return true;
  }

  @Override
  public OperationDb next() {
    if (hasNext()) {
      return iterator.next();
    }
    throw new NoSuchElementException("Operation not found");
  }

  protected abstract List<OperationDb> findOperations(Long tenant, Long idMin, Long idMax);
}
