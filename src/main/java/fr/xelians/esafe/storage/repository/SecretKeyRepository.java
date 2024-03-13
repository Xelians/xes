/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.repository;

import fr.xelians.esafe.storage.entity.SecretKeyDb;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

// Native query is used to avoid caching the secret in the JPA persistence context
public interface SecretKeyRepository extends JpaRepository<SecretKeyDb, Long> {

  @Query(
      value = "SELECT s.secret FROM secret_key_db s WHERE s.tenant = :tenant",
      nativeQuery = true)
  Optional<byte[]> getSecret(@Param("tenant") Long tenant);

  @Transactional
  @Modifying
  @Query(value = "INSERT INTO secret_key_db VALUES(:tenant, :secret)", nativeQuery = true)
  void saveSecret(@Param("tenant") Long tenant, @Param("secret") byte[] secret);

  @Transactional
  @Modifying
  @Query(
      value = "UPDATE secret_key_db s SET s.secret = :secret WHERE s.tenant = :tenant",
      nativeQuery = true)
  void updateSecret(@Param("tenant") Long tenant, @Param("secret") byte[] secret);
}
