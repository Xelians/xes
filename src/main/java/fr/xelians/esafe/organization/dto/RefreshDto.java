/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.dto;

import fr.xelians.esafe.common.constraint.NoHtml;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RefreshDto {

  @NotBlank
  @NoHtml
  @Length(min = 1, max = 16384)
  private String accessToken;

  @NotBlank
  @NoHtml
  @Length(min = 1, max = 256)
  private String refreshToken;

  public RefreshDto() {}

  public RefreshDto(String accessToken, String refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }
}
