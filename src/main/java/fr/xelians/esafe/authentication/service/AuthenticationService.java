/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.service;

import fr.xelians.esafe.authentication.domain.AuthUserDetails;
import fr.xelians.esafe.authentication.domain.JwtProperties;
import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.entity.RefreshTokenDb;
import fr.xelians.esafe.authentication.repository.RefreshTokenRepository;
import fr.xelians.esafe.common.exception.functional.ForbiddenException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.organization.entity.UserDb;
import fr.xelians.esafe.organization.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  public static final String AUTHENTICATION_FAILED = "Authentication failed";

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties properties;

  private SecretKey secretKey;
  private long accessExpireMs;
  private long refreshExpireMs;
  private JwtParser jwtParser;
  private Serializer<Map<String, ?>> jwtSerializer;

  @PostConstruct
  public void init() {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    this.accessExpireMs = properties.getAccessTokenExpiration() * 1000; // Seconds => Milliseconds
    this.refreshExpireMs = properties.getRefreshTokenExpiration() * 1000; // Seconds => Milliseconds
    this.jwtParser = Jwts.parser().verifyWith(secretKey).json(new JacksonDeserializer<>()).build();
    this.jwtSerializer = new JacksonSerializer<>();
  }

  public AccessDto signin(String username, String password) {
    Assert.hasText(username, "Username cannot be empty");
    Assert.hasText(password, "Password cannot be empty");

    // AuthenticationManager authenticates user (with the help of UserDetailService)
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));

    // Keep the authentication in current thread
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Generated the jwtToken and the refreshToken
    AuthUserDetails authUserDetails = (AuthUserDetails) authentication.getPrincipal();
    String accessToken = createJwtToken(authUserDetails.getUsername());
    // TODO Delete remaining refresh token in database if any
    RefreshTokenDb refreshTokenDb = createRefreshToken(authUserDetails.getId());

    return new AccessDto(
        accessToken,
        refreshTokenDb.getToken(),
        authUserDetails.getId(),
        authUserDetails.getUsername(),
        authUserDetails.getEmail());
  }

  // Assign a serializer to avoid a threading bug int the compact() method
  private String createJwtToken(String username) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date(now))
        .expiration(new Date(now + accessExpireMs))
        .signWith(secretKey)
        .json(jwtSerializer)
        .compact();
  }

  // TODO Get the user from the token  not the username
  public String getUsernameFromAccessToken(String accessToken) {
    Assert.hasText(accessToken, "Access token must not be empty");
    return jwtParser.parseSignedClaims(accessToken).getPayload().getSubject();
  }

  private RefreshTokenDb createRefreshToken(Long userId) {
    UserDb userDb =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        AUTHENTICATION_FAILED, String.format("User with id %s not found", userId)));

    RefreshTokenDb refreshTokenDb = new RefreshTokenDb();
    refreshTokenDb.setUser(userDb);
    refreshTokenDb.setExpiryDate(Instant.now().plusMillis(refreshExpireMs));
    refreshTokenDb.setToken(UUID.randomUUID().toString());
    refreshTokenDb = refreshTokenRepository.save(refreshTokenDb);
    return refreshTokenDb;
  }

  // TODO Abort after 30 minutes if not used
  @Transactional(noRollbackFor = ForbiddenException.class)
  public AccessDto refreshToken(String accessToken, String refreshToken) {
    Assert.hasText(refreshToken, "Refresh token cannot be null");

    try {
      jwtParser.parseSignedClaims(accessToken);
      refreshTokenRepository.deleteByToken(refreshToken); // for security
      throw new ForbiddenException(
          AUTHENTICATION_FAILED, "Failed to refresh because access token is not expired");
    } catch (ExpiredJwtException ex) {
      // The refresh token is valid but expired
      // So we can generate a new access token
    } catch (JwtException | IllegalArgumentException ex) {
      refreshTokenRepository.deleteByToken(refreshToken); // for security
      throw new ForbiddenException(
          AUTHENTICATION_FAILED, "Failed to refresh because access token is not valid");
    }

    return refreshTokenRepository
        .findByToken(refreshToken)
        .map(this::validateRefreshToken)
        .map(RefreshTokenDb::getUser)
        .map(user -> new AccessDto(createJwtToken(user.getUsername()), refreshToken))
        .orElseThrow(
            () ->
                new ForbiddenException(
                    AUTHENTICATION_FAILED, "Failed to refresh because refresh token is not found"));
  }

  private RefreshTokenDb validateRefreshToken(RefreshTokenDb refreshTokenDb) {
    if (refreshTokenDb.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(refreshTokenDb);
      throw new ForbiddenException(
          AUTHENTICATION_FAILED, "Failed to refresh because refresh token was expired");
    }
    return refreshTokenDb;
  }

  // For front developpers : don't forget to clear tokens headers in the browser
  @Transactional
  public void logout(Long userId) {
    Assert.notNull(userId, "UsedId cannot be null");

    UserDb userDb =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        AUTHENTICATION_FAILED, String.format("User with id %s not found", userId)));

    refreshTokenRepository.deleteByUser(userDb);
  }
}
