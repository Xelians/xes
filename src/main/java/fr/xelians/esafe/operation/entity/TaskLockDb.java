/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(name = "task_lock")
public class TaskLockDb {

  @Id
  @NotNull
  @SequenceGenerator(name = "task_lock_generator", sequenceName = "task_lock_seq")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_lock_generator")
  private Long id;

  @NotNull private boolean exclusiveLock = false;

  // Needed by JPA
  public TaskLockDb() {}

  public TaskLockDb(Long tenant) {
    this.id = tenant;
  }
}
