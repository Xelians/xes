/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.objectgroup.MetadataModel;
import fr.gouv.vitam.common.model.objectgroup.PhysicalDimensionsModel;
import fr.gouv.vitam.common.model.objectgroup.StorageJson;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VersionsDto {

  @JsonProperty("#rank")
  @JsonAlias({"_rank"})
  private Integer rank;

  @JsonProperty("#id")
  @JsonAlias({"_id"})
  private String id;

  @JsonProperty("DataObjectVersion")
  private String dataObjectVersion;

  @JsonProperty("DataObjectGroupId")
  private String dataObjectGroupId;

  @JsonProperty("FormatIdentification")
  private FormatIdentificationModel formatIdentification;

  @JsonProperty("FileInfo")
  private FileInfoModel fileInfoModel;

  @JsonProperty("Metadata")
  private MetadataModel metadata;

  @JsonProperty("Size")
  private Long size;

  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("MessageDigest")
  private String messageDigest;

  @JsonProperty("Algorithm")
  private String algorithm;

  @JsonProperty("#storage")
  @JsonAlias({"_storage"})
  private StorageJson storage;

  @JsonProperty("PhysicalDimensions")
  private PhysicalDimensionsModel physicalDimensionsModel;

  @JsonProperty("PhysicalId")
  private String physicalId;

  @JsonProperty("OtherMetadata")
  private Map<String, Object> otherMetadata = new HashMap<>();

  @JsonProperty("#opi")
  @JsonAlias({"_opi"})
  private String opi;

  @JsonProperty("DataObjectProfile")
  private String dataObjectProfile;
}
