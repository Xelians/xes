/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.dto;

import fr.xelians.esafe.common.constraint.NoHtml;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class LoginDto {

  @NotBlank
  @NoHtml
  @Length(min = 1, max = 256)
  private String username;

  @NotBlank
  @NoHtml
  @Length(min = 1, max = 256)
  private String password;

  public LoginDto() {}

  public LoginDto(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
