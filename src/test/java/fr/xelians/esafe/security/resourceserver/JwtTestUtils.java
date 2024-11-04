package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_ADMIN;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_MANAGER;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_READER;
import static fr.xelians.esafe.security.JwtClaimNames.*;

import fr.xelians.esafe.security.grantedauthority.TenantGrantedAuthority;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtTestUtils {
  public static final String JWT_TOKEN_VALUE = "jwt-token-value";

  public static final String ORGANIZATION_ID_VALUE = "org-123456";

  public static final String USER_ID_VALUE = "user-987654321";

  public static final long IAT_VALUE = Instant.now().toEpochMilli();

  public static final long EXP_VALUE = Instant.now().plusSeconds(60).toEpochMilli();

  public static final List<String> GLOBAL_ROLES_VALUES = List.of(ROLE_ADMIN);

  public static final Long TENANT_NUMBER_ONE = 1L;

  public static final Long TENANT_NUMBER_TWO = 2L;

  public static final Map<String, List<String>> TENANT_ROLES_VALUES =
      Map.of(
          TENANT_NUMBER_ONE.toString(),
          List.of(ROLE_ARCHIVE_READER),
          TENANT_NUMBER_TWO.toString(),
          List.of(ROLE_ARCHIVE_MANAGER));

  public static final TenantGrantedAuthority TENANT_ONE_GRANTED_AUTHORITY =
      new TenantGrantedAuthority(TENANT_NUMBER_ONE, ROLE_ARCHIVE_READER);

  public static final TenantGrantedAuthority TENANT_TWO_GRANTED_AUTHORITY =
      new TenantGrantedAuthority(TENANT_NUMBER_TWO, ROLE_ARCHIVE_MANAGER);

  public static final SimpleGrantedAuthority GLOBAL_GRANTED_AUTHORITY =
      new SimpleGrantedAuthority(ROLE_ADMIN);

  public static final Map<String, Object> HEADERS;

  public static final Map<String, Object> CLAIMS;

  public static final Jwt JWT;

  static {
    HEADERS = new HashMap<>();
    HEADERS.put("alg", JwsAlgorithms.RS256);

    CLAIMS = new HashMap<>();
    CLAIMS.put(ORGANIZATION_ID, ORGANIZATION_ID_VALUE);
    CLAIMS.put(USER_ID, USER_ID_VALUE);
    CLAIMS.put(ROLES, Map.of(GLOBAL_ROLES, GLOBAL_ROLES_VALUES, TENANT_ROLES, TENANT_ROLES_VALUES));

    JWT =
        new Jwt(
            JWT_TOKEN_VALUE,
            Instant.ofEpochMilli(IAT_VALUE),
            Instant.ofEpochMilli(EXP_VALUE),
            HEADERS,
            CLAIMS);
  }
}
