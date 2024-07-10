/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.common.dto.AbstractBaseDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

/*
   This abstract class represents the base DTO for referential.
   The annotations @NotBlank and @NotNull specify mandatory properties.
   Default values must be set for non-mandatory properties except empty ones.
*/

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class AbstractReferentialDto extends AbstractBaseDto implements ReferentialDto {

  // The identifier is automatically created if it does not exist
  // Beware - it's a bad practice to rely on automatic creation
  @NoHtml
  @RegularChar
  @Length(min = 1, max = 64)
  @JsonProperty("Identifier")
  protected String identifier;

  @JsonProperty("#tenant")
  protected Long tenant;
}
