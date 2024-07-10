/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.entity;

import fr.xelians.esafe.organization.entity.UserDb;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "refresh_token",
    indexes = {@Index(columnList = "token")})
public class RefreshTokenDb {

  @Id
  @SequenceGenerator(name = "refresh_token_generator", sequenceName = "refresh_token_seq")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_generator")
  private long id;

  @ManyToOne
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @NotNull
  private UserDb user;

  @Column(nullable = false, unique = true)
  @NotBlank
  private String token;

  @NotNull private Instant expiryDate;
}
