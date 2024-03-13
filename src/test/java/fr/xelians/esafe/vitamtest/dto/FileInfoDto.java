/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileInfoDto {

  @JsonProperty("Filename")
  private String filename;

  @JsonProperty("CreatingApplicationName")
  private String creatingApplicationName;

  @JsonProperty("CreatingApplicationVersion")
  private String creatingApplicationVersion;

  @JsonProperty("CreatingOs")
  private String creatingOs;

  @JsonProperty("CreatingOsVersion")
  private String creatingOsVersion;

  @JsonProperty("LastModified")
  private String lastModified;

  @JsonProperty("DateCreatedByApplication")
  private String dateCreatedByApplication;
}
