/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class ValueDetail {

  @JsonCreator
  public ValueDetail() {}

  public ValueDetail(long ingested) {
    this.ingested = ingested;
    this.deleted = 0L;
    this.remained = ingested;
  }

  @JsonProperty("ingested")
  private Long ingested;

  @JsonProperty("deleted")
  private Long deleted;

  @JsonProperty("remained")
  private Long remained;

  @JsonIgnore
  public void addIngested(long ingested) {
    this.ingested += ingested;
    this.remained += ingested;
  }

  @JsonIgnore
  public void addDeleted(long deleted) {
    this.deleted += deleted;
    this.remained += deleted;
  }
}
