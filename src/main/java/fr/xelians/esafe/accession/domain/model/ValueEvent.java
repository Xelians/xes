/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@ToString
public class ValueEvent {

  @JsonProperty("Opc")
  private Long operationId;

  @JsonProperty("OpType")
  private String operationType;

  @JsonProperty("Gots")
  private Long objectsGroups;

  @JsonProperty("Units")
  private Long totalUnits;

  @JsonProperty("Objects")
  private Long totalObjects;

  @JsonProperty("ObjSize")
  private Long objectSize;

  @JsonProperty("CreationDate")
  private LocalDateTime creationDate;
}
