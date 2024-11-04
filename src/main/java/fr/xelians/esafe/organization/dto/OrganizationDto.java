/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.common.dto.AbstractBaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OrganizationDto extends AbstractBaseDto {

  // The identifier is mandatory
  @NoHtml
  @NotBlank
  @RegularChar
  @Length(min = 1, max = 64)
  @JsonProperty("Identifier")
  protected String identifier;

  @JsonProperty("Tenant")
  protected Long tenant;
}
