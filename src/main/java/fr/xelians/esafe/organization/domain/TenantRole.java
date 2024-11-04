/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.exception.technical.InternalException;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
public class TenantRole {

  @JsonProperty("Tenant")
  @NotNull
  private Long tenant;

  @JsonProperty("RoleName")
  @NotNull
  private Role.TenantRole roleName;

  @JsonCreator
  public TenantRole(
      @JsonProperty("Tenant") Long tenant, @JsonProperty("RoleName") Role.TenantRole roleName) {
    this.tenant = tenant;
    this.roleName = roleName;
  }

  public TenantRole(String str) {
    String[] tks = StringUtils.split(str, ":", 2);
    if (tks.length != 2) {
      throw new InternalException(String.format("Bad tenant contract '%s", str));
    }
    this.tenant = Long.parseLong(tks[0]);
    this.roleName = Role.TenantRole.valueOf(tks[1]);
  }

  public String toString() {
    return tenant + ":" + roleName;
  }
}
