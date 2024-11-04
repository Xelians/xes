/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.cluster.entity;

import fr.xelians.esafe.cluster.domain.JobType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(name = "job")
public class JobDb {

  @Id
  @Enumerated(EnumType.STRING)
  @Column(unique = true, nullable = false)
  private JobType jobType;

  @Column private Long identifier;

  @Column @NotNull private LocalDateTime expire;
}
