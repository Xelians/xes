/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.cluster.entity;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "server_node")
public class ServerNodeDb {

  // TODO Fix this hack
  @SequenceGenerator(
      name = "server_node_generator",
      sequenceName = "server_node_seq",
      allocationSize = 1)
  @GeneratedValue(generator = "server_node_generator")
  @Id
  @Enumerated(EnumType.STRING)
  @Column(unique = true, nullable = false)
  private NodeFeature feature;

  @Column private Long identifier;

  @Column @NotNull private LocalDateTime delay;
}
