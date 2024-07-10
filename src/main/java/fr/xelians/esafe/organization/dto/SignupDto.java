/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class SignupDto {

  @NotNull
  @JsonProperty("Organization")
  private OrganizationDto organizationDto;

  @NotNull
  @JsonProperty("User")
  private UserDto userDto;

  @NotNull
  @JsonProperty("Tenant")
  private TenantDto tenantDto;
}
