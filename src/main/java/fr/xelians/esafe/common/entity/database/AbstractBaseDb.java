/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.entity.database;

import static fr.xelians.esafe.common.constant.DefaultValue.*;

import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.referential.domain.Status;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractBaseDb implements BaseDb {

  @NotNull
  @Length(min = 1, max = 512)
  @Column(length = 512)
  protected String name;

  @NotNull
  @Column(length = 512)
  @Length(max = 512)
  protected String description = "";

  @Column(nullable = false, updatable = false)
  @NotNull
  protected LocalDate creationDate = DefaultValue.creationDate();

  @NotNull protected LocalDate lastUpdate = DefaultValue.lastUpdate();

  @NotNull protected LocalDate activationDate = ACTIVATION_DATE;

  @NotNull protected LocalDate deactivationDate = DEACTIVATION_DATE;

  @NotNull protected Status status = BASE_STATUS;

  protected byte[] lfcs = null;

  @NotNull protected Integer autoVersion = AUTO_VERSION;

  @NotNull protected Long operationId;

  @PrePersist
  @PreUpdate
  void preInsert() {
    if (description == null) description = "";
    if (creationDate == null) DefaultValue.creationDate();
    if (lastUpdate == null) DefaultValue.lastUpdate();
    if (activationDate == null) activationDate = ACTIVATION_DATE;
    if (deactivationDate == null) deactivationDate = DEACTIVATION_DATE;
    if (status == null) status = BASE_STATUS;
    if (autoVersion == null) autoVersion = AUTO_VERSION;
  }
}
