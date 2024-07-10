/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.cluster.entity;

import fr.xelians.esafe.cluster.domain.MessageContent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "message")
public class MessageDb {

  @Id
  @SequenceGenerator(name = "message_generator", sequenceName = "message_seq")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_generator")
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
