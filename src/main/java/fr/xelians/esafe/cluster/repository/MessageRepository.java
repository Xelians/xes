/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.cluster.repository;

import fr.xelians.esafe.cluster.entity.MessageDb;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<MessageDb, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(
      @QueryHint(
          name = AvailableSettings.JAKARTA_LOCK_TIMEOUT,
          value = "" + LockOptions.SKIP_LOCKED))
  @Query(
      "select o from MessageDb o where o.recipient=:id or o.recipient in (:features) order by o.id asc")
  List<MessageDb> findByIdentifierOrFeatures(
      @Param("id") String id, @Param("features") List<String> features);
}
