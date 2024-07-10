/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AbstractVitamUIResponseDto<T> {

  @JsonProperty("httpCode")
  private String httpCode;

  @JsonProperty("$hits")
  private HitsDto hits;

  @JsonProperty("$results")
  private List<T> results = new ArrayList<>();
}
