/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Text {

  @NonNull
  @JsonProperty("Message")
  protected final String message;

  @JsonProperty("Lang")
  protected final String lang;

  public Text(String message) {
    this(message, null);
  }

  @JsonCreator
  public Text(@JsonProperty("Message") @NonNull String message, @JsonProperty("Lang") String lang) {
    this.message = message;
    this.lang = lang;
  }
}
