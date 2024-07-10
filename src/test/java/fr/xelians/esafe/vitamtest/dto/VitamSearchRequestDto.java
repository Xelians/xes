/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.QueryDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VitamSearchRequestDto {

  @JsonProperty("$roots")
  private List<String> roots = new ArrayList<>();

  @JsonProperty("$query")
  private List<QueryDTO> query = new ArrayList<>();

  @JsonProperty("$filter")
  private FilterDto filter;

  @JsonProperty("$projection")
  private ProjectionDto projection;
}
