/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.cluster.entity;

import fr.xelians.esafe.cluster.domain.MessageContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class MessageDb {

  @GeneratedValue(generator = "global_generator")
  @Id
  private Long id;

  @Column(nullable = false, updatable = false)
  @NotNull
  private Long senderIdentifier;

  @Column(nullable = false, updatable = false)
  @NotNull
  private String recipient;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  @NotNull
  private MessageContent content;

  @Column(nullable = false, updatable = false)
  @NotNull
  private LocalDateTime created;
}
