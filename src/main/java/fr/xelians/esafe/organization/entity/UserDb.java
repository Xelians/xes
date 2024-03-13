/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import fr.xelians.esafe.organization.domain.role.GlobalRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * Note. OpenId : https://openid.net/specs/openid-connect-core-1_0.html#IDToken
 *
 * <p>Note. Using OneToMany join is definitively a bad practice. See
 * https://dev.to/alagrede/why-i-dont-want-use-jpa-anymore-fl
 * https://dev.to/schmowser/onetomany-relations-in-hibernate-and-its-perils-1a41
 *
 * <p>Note. To handle Optimistic Locking each entity class must have only one version attribute that
 * must be placed in the primary table for an entity mapped to several tables. The type of a version
 * attribute must be one of the following: int, Integer, long, Long, short, Short,
 * java.sql.Timestamp See http://www.baeldung.com/jpa-optimistic-locking
 *
 * <p>To manage concurrency in a distributed Restful environment, see
 * https://blog.novatec-gmbh.de/managing-concurrency-in-a-distributed-restful-environment-with-spring-boot-and-angular2/
 */
@Getter
@Setter
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_user_identifier_organization_id",
          columnNames = {"identifier", "organization_id"}),
      @UniqueConstraint(
          name = "unique_user_username",
          columnNames = {"username"}),
      @UniqueConstraint(
          name = "unique_user_email",
          columnNames = {"email"})
    })
@Entity
public class UserDb extends AbstractBaseDb {

  @Id
  @GeneratedValue(generator = "global_generator")
  protected Long id;

  @Column(nullable = false, updatable = false)
  @NotNull
  @Length(min = 1, max = 64)
  protected String identifier;

  @Column(nullable = false)
  @NotBlank
  @Length(min = 1, max = 256)
  private String username;

  @NotBlank
  @Length(min = 1, max = 256)
  private String firstName;

  @NotBlank
  @Length(min = 1, max = 256)
  private String lastName;

  @Column(nullable = false)
  @NotBlank
  @Email
  @Length(min = 1, max = 256)
  private String email;

  @NotBlank
  @Length(min = 8, max = 256)
  private String password;

  @NotNull private HashSet<String> apiKey = new HashSet<>();

  @NotNull private HashSet<GlobalRole> globalRoles = new HashSet<>();

  @NotNull private HashSet<String> tenantRoles = new HashSet<>();

  @NotNull private HashSet<String> accessContracts = new HashSet<>();

  @NotNull private HashSet<String> ingestContracts = new HashSet<>();

  /** Many users must link the same organization */
  @JsonIgnore
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private OrganizationDb organization;
}
