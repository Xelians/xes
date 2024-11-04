/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArchiveTransfer extends Message {

  @JsonProperty("ArchivalAgreement")
  protected String archivalAgreement;

  @JsonProperty("ArchivalAgency")
  protected Agency archivalAgency;

  @JsonProperty("TransferringAgency")
  protected Agency transferringAgency;

  @JsonProperty("Created")
  protected LocalDateTime created;

  @JsonProperty("Tenant")
  protected Long tenant;

  @JsonProperty("OperationId")
  protected Long operationId;
}
