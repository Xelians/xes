/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import lombok.Getter;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

@Getter
public enum Hash {
  MD5(MessageDigestAlgorithms.MD5),
  SHA256(MessageDigestAlgorithms.SHA_256),
  SHA512(MessageDigestAlgorithms.SHA_512);

  private final String algorithm;

  Hash(String algorithm) {
    this.algorithm = algorithm;
  }
}
