/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import fr.xelians.esafe.common.exception.technical.InternalException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * @author Emmanuel Deviller
 */
public class Aes {

  public static final String AES_ALGORITHM = "AES";
  public static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

  public static SecretKey createSecretKey(String secret, byte[] salt) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
      KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 65536, 256);
      SecretKey tmp = factory.generateSecret(spec);
      return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
      throw new InternalException(
          "AES secret initialisation failed", "Failed to create secret key from key generator", ex);
    }
  }

  public static SecretKey createSecretKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
      keyGenerator.init(256);
      return keyGenerator.generateKey();
    } catch (NoSuchAlgorithmException ex) {
      throw new InternalException(
          "AES secret initialisation failed", "Failed to create secret key from key generator", ex);
    }
  }
}
