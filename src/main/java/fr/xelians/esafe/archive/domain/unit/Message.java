/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Message {

  @JsonProperty("Date")
  protected LocalDateTime date;

  @JsonProperty("MessageIdentifier")
  protected String messageIdentifier;

  @JsonProperty("Comment")
  protected String comment;
}
