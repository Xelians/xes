/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class AgentTypeDto {

  @JsonProperty("Identifier")
  private List<String> identifier = new ArrayList<>();

  public Optional<String> retrieveFirstIdentifier() {
    return Optional.ofNullable(identifier).orElse(Collections.emptyList()).stream().findFirst();
  }
}
