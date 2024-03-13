/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.repository;

import fr.xelians.esafe.authentication.entity.RefreshTokenDb;
import fr.xelians.esafe.organization.entity.UserDb;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenDb, Long> {

  Optional<RefreshTokenDb> findByToken(String token);

  int deleteByUser(UserDb user);

  int deleteByToken(String token);
}
