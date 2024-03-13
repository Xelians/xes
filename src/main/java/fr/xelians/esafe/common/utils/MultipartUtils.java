/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.common.exception.functional.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

public class MultipartUtils {

  private MultipartUtils() {}

  public static byte[] toBytes(InputStream is, int maxLength) throws IOException {
    return is.readNBytes(maxLength);
  }

  public static byte[] toBytes(MultipartFile multiPartFile, int maxLength) throws IOException {
    Assert.notNull(multiPartFile, "Multipart file must be not null");

    try (InputStream inputStream = multiPartFile.getInputStream()) {
      byte[] bytes = inputStream.readNBytes(maxLength + 1);
      if (bytes.length > maxLength) {
        throw new BadRequestException(
            String.format(
                "Failed to upload because file size is greater than allowed size: '%d' bytes",
                maxLength));
      }
      return bytes;
    }
  }
}
