/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.sequence;

import fr.xelians.esafe.operation.entity.OperationDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SequenceRepository extends JpaRepository<OperationDb, Long> {

  @Query(value = "select currval('global')", nativeQuery = true)
  long getCurrentValue();

  @Query(value = "select nextval('global')", nativeQuery = true)
  long getNextValue();
}
