/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.storage.domain.Aes;
import fr.xelians.esafe.storage.repository.SecretKeyRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecretKeyService {

  private static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  private static final byte[] MASTER_REF =
      "this is the tenant encryption secret reference".getBytes(StandardCharsets.UTF_8);
  private static final Long TENANT_REF = -1L;

  private final SecretKeyRepository repository;

  // We can cache secretKeys because the master key does not change
  @Value("${app.tenant.encryption.expiration:0}")
  private long expTime;

  private LoadingCache<Long, SecretKey> secretKeyCache;

  // TODO do not use SpringBoot Property to load sensitive data
  @Value("${app.tenant.encryption.secret}")
  private String secret;

  private SecretKey masterKey;

  @PostConstruct
  public void init() {
    if (expTime > 0) {
      this.secretKeyCache = initCache();
    }
    this.masterKey = Aes.createSecretKey(secret);
    this.secret =
        null; // That is not very useful because it is also stored in the SpringBoot context

    // Check if masterKey is valid
    Optional<byte[]> optSecret = repository.getSecret(TENANT_REF);
    try {
      if (optSecret.isPresent()) {
        byte[] decMasterRef = Aes.decrypt(optSecret.get(), masterKey);
        if (!Arrays.equals(MASTER_REF, decMasterRef)) {
          throw new InternalException(
              "Failed to check tenant encryption secret property",
              "Secret property does not match secret reference");
        }
      } else {
        byte[] encMasterRef = Aes.encrypt(MASTER_REF, masterKey);
        repository.saveSecret(TENANT_REF, encMasterRef);
      }
    } catch (IOException e) {
      throw new InternalException(
          "Failed to check tenant encryption secret property",
          "Failed to encrypt or decrypt secret reference",
          e);
    }
  }

  public SecretKey getSecretKey(Long tenant) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return expTime > 0 ? getSecretKeyFromCache(tenant) : getSecretKeyFromDb(tenant);
  }

  @Transactional(rollbackFor = Exception.class)
  public void saveSecretKey(Long tenant, SecretKey secretKey) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(secretKey, "Secret must be not null");

    Optional<byte[]> optSecret = repository.getSecret(tenant);
    if (optSecret.isPresent()) {
      throw new InternalException(
          "Failed to save secret", String.format("Secret already exists for tenant '%s'", tenant));
    }

    try {
      byte[] encSecret = Aes.encrypt(secretKey.getEncoded(), masterKey);
      repository.saveSecret(tenant, encSecret);
    } catch (IOException e) {
      throw new InternalException(
          "Failed to save secret", String.format("Failed to encrypt secret for '%s'", tenant), e);
    }
  }

  private SecretKey getSecretKeyFromDb(Long tenant) {
    try {
      byte[] decSecret = Aes.decrypt(getSecret(tenant), masterKey);
      return new SecretKeySpec(decSecret, 0, decSecret.length, Aes.AES_ALGORITHM);
    } catch (IOException e) {
      throw new InternalException(
          "Failed to get secret", String.format("Failed to decrypt secret for '%s'", tenant), e);
    }
  }

  private byte[] getSecret(Long tenant) {
    return repository
        .getSecret(tenant)
        .orElseThrow(
            () ->
                new InternalException(
                    "Secret not found", String.format("Failed to find tenant '%s'", tenant)));
  }

  private SecretKey getSecretKeyFromCache(Long tenant) {
    try {
      return secretKeyCache.get(tenant);
    } catch (ExecutionException e) {
      throw new InternalException(
          "Failed to get secret key",
          String.format("Failed to get or load value from cache for tenant '%s'", tenant),
          e);
    }
  }

  private LoadingCache<Long, SecretKey> initCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(expTime, TimeUnit.SECONDS)
        .build(
            new CacheLoader<>() {
              public SecretKey load(Long key) {
                return getSecretKeyFromDb(key);
              }
            });
  }
}
