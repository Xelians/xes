/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@JsonDeserialize(builder = FileInfoBuilder.class)
public class FileInfo {

  @JsonProperty("Filename")
  private String filename;

  @JsonProperty("CreatingApplicationName")
  private String creatingApplicationName;

  @JsonProperty("CreatingApplicationVersion")
  private String creatingApplicationVersion;

  @JsonProperty("DateCreatedByApplication")
  private LocalDateTime dateCreatedByApplication;

  @JsonProperty("CreatingOs")
  private String creatingOs;

  @JsonProperty("CreatingOsVersion")
  private String creatingOsVersion;

  @JsonProperty("LastModified")
  private LocalDateTime lastModified;

  public FileInfo() {}

  public FileInfo(
      @JsonProperty("Filename") String filename,
      @JsonProperty("CreatingApplicationName") String creatingApplicationName,
      @JsonProperty("CreatingApplicationVersion") String creatingApplicationVersion,
      @JsonProperty("DateCreatedByApplication") LocalDateTime dateCreatedByApplication,
      @JsonProperty("CreatingOs") String creatingOs,
      @JsonProperty("CreatingOsVersion") String creatingOsVersion,
      @JsonProperty("LastModified") LocalDateTime lastModified) {

    this.filename = filename;
    this.creatingApplicationName = creatingApplicationName;
    this.creatingApplicationVersion = creatingApplicationVersion;
    this.dateCreatedByApplication = dateCreatedByApplication;
    this.creatingOs = creatingOs;
    this.creatingOsVersion = creatingOsVersion;
    this.lastModified = lastModified;
  }
}
