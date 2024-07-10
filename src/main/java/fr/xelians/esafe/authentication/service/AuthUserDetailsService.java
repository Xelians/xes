/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.service;

import fr.xelians.esafe.authentication.domain.AuthUserDetails;
import fr.xelians.esafe.organization.domain.role.GlobalRole;
import fr.xelians.esafe.organization.entity.UserDb;
import fr.xelians.esafe.organization.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Assert.notNull(username, "Username cannot be null");

    UserDb userDb =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));

    return new AuthUserDetails(userDb);
  }

  public Optional<AuthUserDetails> loadUserByApiKey(String apiKey)
      throws UsernameNotFoundException {
    Assert.notNull(apiKey, "apiKey cannot be null");

    //  FIXME
    //  Replace by a repository QUERY
    return userRepository.findAll().stream()
        .filter(
            user ->
                user.getApiKey().contains(apiKey)
                    && user.getGlobalRoles().contains(GlobalRole.ROLE_ADMIN))
        .findFirst()
        .map(AuthUserDetails::new);
  }

  public AuthUserDetails getAuthUserDetails(String username) {
    return (AuthUserDetails) loadUserByUsername(username);
  }
}
