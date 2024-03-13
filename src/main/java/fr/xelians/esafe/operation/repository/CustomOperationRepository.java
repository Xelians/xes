/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.repository;

import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationQuery;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

public interface CustomOperationRepository {

  Slice<OperationStatusDto> findOperationStatus(
      Long tenant, OperationQuery operationQuery, PageRequest limit);

  Slice<OperationDto> searchOperationDtos(
      Long tenant, OperationQuery operationQuery, PageRequest limit);
}
