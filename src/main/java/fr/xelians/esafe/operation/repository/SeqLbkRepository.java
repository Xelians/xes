/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.repository;

import fr.xelians.esafe.operation.entity.DummyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeqLbkRepository extends JpaRepository<DummyEntity, Long> {

  @Query(value = "select currval('logbook')", nativeQuery = true)
  long getCurrentValue();

  @Query(value = "select nextval('logbook')", nativeQuery = true)
  long getNextValue();
}
