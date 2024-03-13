/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BinaryDataObject {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  @NonNull
  protected String xmlId;

  @JsonProperty("_binaryId")
  protected Long id;

  @JsonProperty("_opi")
  protected Long operationId;

  @JsonProperty("_pos")
  protected long[] pos = {-1, -1};

  @JsonIgnore protected Path binaryPath;

  @JsonProperty("BinaryVersion")
  protected String binaryVersion;

  @JsonProperty("FormatIdentification")
  protected FormatIdentification formatIdentification;

  @NonNull
  @JsonProperty("MessageDigest")
  protected String messageDigest;

  @JsonProperty("Size")
  protected long size;

  @NonNull
  @JsonProperty("DigestAlgorithm")
  protected String digestAlgorithm = "SHA-512";

  @JsonProperty("FileInfo")
  protected FileInfo fileInfo;
}
