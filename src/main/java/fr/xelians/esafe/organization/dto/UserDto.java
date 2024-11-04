/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.dto;

import static fr.xelians.esafe.organization.domain.Role.GlobalRole.ROLE_ROOT_ADMIN;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.common.dto.AbstractBaseDto;
import fr.xelians.esafe.organization.domain.Role;
import fr.xelians.esafe.organization.domain.TenantRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import lombok.*;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserDto extends AbstractBaseDto {

  // The identifier is mandatory
  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 64)
  @JsonProperty("Identifier")
  protected String identifier;

  @NoHtml
  @NotBlank
  @RegularChar
  @Size(min = 3, max = 256)
  @JsonProperty("UserName")
  private String username;

  @NotBlank
  @Size(min = 8, max = 256)
  @JsonProperty("Password")
  private String password;

  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 256)
  @JsonProperty("FirstName")
  private String firstName;

  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 256)
  @JsonProperty("LastName")
  private String lastName;

  @Email
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 256)
  @JsonProperty("Email")
  private String email;

  @Size(max = 1000)
  @JsonProperty("GlobalRoles")
  private ArrayList<Role.GlobalRole> globalRoles = new ArrayList<>();

  @Size(max = 1000)
  @JsonProperty("TenantRoles")
  private ArrayList<TenantRole> tenantRoles = new ArrayList<>();

  @Size(max = 1000)
  @JsonProperty("AccessContracts")
  private ArrayList<TenantContract> accessContracts = new ArrayList<>();

  @Size(max = 1000)
  @JsonProperty("IngestContracts")
  private ArrayList<TenantContract> ingestContracts = new ArrayList<>();

  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 64)
  @JsonProperty("OrganizationIdentifier")
  private String organizationIdentifier;

  @JsonIgnore
  public String getPassword() {
    return password;
  }

  @JsonIgnore
  public boolean isRootAdmin() {
    return globalRoles.contains(ROLE_ROOT_ADMIN);
  }
}
