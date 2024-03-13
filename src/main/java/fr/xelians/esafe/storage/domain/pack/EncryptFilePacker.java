/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.pack;

import fr.xelians.esafe.storage.domain.Aes;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.Validate;

public class EncryptFilePacker implements Packer {

  private final Path dstPath;
  private OutputStream dstStream;
  private long end;
  private SecretKey secretKey;

  public EncryptFilePacker(SecretKey secretKey, Path dstPath) {
    Validate.notNull(dstPath, "dstPath");
    this.dstPath = dstPath;
    this.secretKey = secretKey;
  }

  @Override
  public long[] write(Path srcPath) throws IOException {
    if (dstStream == null) {
      dstStream = Files.newOutputStream(dstPath);
    }

    try {
      return doWrite(srcPath);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IOException(e);
    }
  }

  private long[] doWrite(Path srcPath)
      throws IOException, IllegalBlockSizeException, BadPaddingException {
    long start = end;
    long size = 0;

    byte[] buffer = new byte[16 * 128];
    byte[] output;

    Cipher cipher = Aes.createCipher();
    byte[] ivBytes = Aes.initWriteCipher(cipher, secretKey);

    try (InputStream is = Files.newInputStream(srcPath)) {

      // Prepend initialization vector
      dstStream.write(ivBytes);
      size += ivBytes.length;

      // Encrypt stream
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        output = cipher.update(buffer, 0, bytesRead);
        if (output != null) {
          dstStream.write(output);
          size += output.length;
        }
      }
      output = cipher.doFinal();
      if (output != null) {
        dstStream.write(output);
        size += output.length;
      }
    }

    if (size > 0) {
      end = start + size;
      return new long[] {start, end - 1};
    }
    return new long[] {-1, -1};
  }

  @Override
  public void close() throws IOException {
    secretKey = null;
    if (dstStream != null) {
      dstStream.close();
    }
  }
}
