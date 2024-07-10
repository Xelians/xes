/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.dto;

import static fr.xelians.esafe.common.constant.DefaultValue.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.referential.domain.Status;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class AbstractBaseDto implements BaseDto {

  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 256)
  @JsonProperty("Name")
  protected String name;

  @NoHtml
  @RegularChar
  @Length(max = 512)
  @JsonProperty("Description")
  protected String description = "";

  /* Accepted dates format for deserialization:
   *    - yyyy-MM-dd (ex. "1986-12-21")
   *    - d[d]/M[M]/yyyy (ex. "21/12/1976" or "3/2/1986")
   *    - ISO Date Time format
   *
   * Default date format for serialization: yyyy-MM-dd
   *
   * Both JsonDeserialize and JsonFormat annotations are mandatory
   * */

  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonProperty("CreationDate")
  protected LocalDate creationDate = DefaultValue.creationDate();

  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonProperty("LastUpdate")
  protected LocalDate lastUpdate = DefaultValue.lastUpdate();

  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonProperty("ActivationDate")
  protected LocalDate activationDate = ACTIVATION_DATE;

  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonProperty("DeactivationDate")
  protected LocalDate deactivationDate = DEACTIVATION_DATE;

  @JsonProperty("Status")
  protected Status status = BASE_STATUS;

  @JsonProperty("LifeCycles")
  protected List<LifeCycle> lifeCycles = null;

  @JsonProperty("_av")
  protected int autoVersion = AUTO_VERSION;

  @JsonProperty("_opi")
  protected Long operationId;
}
