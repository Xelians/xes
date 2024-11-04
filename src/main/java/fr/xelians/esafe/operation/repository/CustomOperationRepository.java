/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.repository;

import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationQuery;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/*
 * @author Emmanuel Deviller
 */
public interface CustomOperationRepository {

  Slice<OperationStatusDto> findOperationStatus(
      Long tenant, OperationQuery operationQuery, PageRequest limit);

  Slice<OperationDto> searchOperationDtos(
      Long tenant, OperationQuery operationQuery, PageRequest limit);
}
