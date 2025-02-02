/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.unit;

import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public class UnitDbIterator extends DbIterator {

  public UnitDbIterator(Long tenant, long idMax, OperationService operationService) {
    super(tenant, idMax, operationService);
  }

  @Override
  public List<OperationDb> findOperations(Long tenant, Long idMin, Long idMax) {
    return operationService.findForReindex(tenant, idMin, idMax);
  }
}
