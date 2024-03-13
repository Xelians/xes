/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.cluster.entity;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@SequenceGenerator(name = "server_node_generator", sequenceName = "server_node", allocationSize = 1)
@Getter
@Setter
@Entity
public class ServerNodeDb {

  // TODO Fix this hack
  @GeneratedValue(generator = "server_node_generator")
  @Id
  @Enumerated(EnumType.STRING)
  @Column(unique = true, nullable = false)
  private NodeFeature feature;

  @Column private Long identifier;

  @Column @NotNull private LocalDateTime delay;
}
