/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * La classe FileInfoBuilder facilite la création d'un objet FileInfo en suivant le principe de
 * conception du pattern builder.
 *
 * @author Emmanuel Deviller
 * @see FileInfo
 * @author Emmanuel Deviller
 */
public class FileInfoBuilder {

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

  private FileInfoBuilder() {}

  /**
   * Instancie le builder.
   *
   * @return le builder
   */
  public static FileInfoBuilder builder() {
    return new FileInfoBuilder();
  }

  /**
   * Spécifie le nom du fichier d'origine.
   *
   * @param filename le nom du fichier d'origine
   * @return le builder
   */
  public FileInfoBuilder withFilename(@JsonProperty("Filename") String filename) {
    this.filename = filename;
    return this;
  }

  /**
   * Spécifie le nom de l'application utilisée pour créer le fichier.
   *
   * @param creatingApplicationName le nom de l'application utilisée pour créer le fichier
   * @return le builder
   */
  public FileInfoBuilder withCreatingApplicationName(
      @JsonProperty("CreatingApplicationName") String creatingApplicationName) {
    this.creatingApplicationName = creatingApplicationName;
    return this;
  }

  /**
   * Spécifie la version de l'application utilisée pour créer le fichier.
   *
   * @param creatingApplicationVersion la version de l'application utilisée pour créer le fichier
   * @return le builder
   */
  public FileInfoBuilder withCreatingApplicationVersion(
      @JsonProperty("CreatingApplicationVersion") String creatingApplicationVersion) {
    this.creatingApplicationVersion = creatingApplicationVersion;
    return this;
  }

  /**
   * Spécifie la date de création du fichier.
   *
   * @param dateCreatedByApplication la date de création du fichier
   * @return le builder
   */
  public FileInfoBuilder withDateCreatedByApplication(
      @JsonProperty("DateCreatedByApplication") LocalDateTime dateCreatedByApplication) {
    this.dateCreatedByApplication = dateCreatedByApplication;
    return this;
  }

  /**
   * Spécifie le système d’exploitation utilisé pour créer le fichier.
   *
   * @param creatingOs le système d’exploitation utilisé pour créer le fichier
   * @return le builder
   */
  public FileInfoBuilder withCreatingOs(@JsonProperty("CreatingOs") String creatingOs) {
    this.creatingOs = creatingOs;
    return this;
  }

  /**
   * Spécifie la version du système d'exploitation utilisé pour créer le fichier.
   *
   * @param creatingOsVersion la version du système d'exploitation utilisé pour créer le fichier
   * @return le builder
   */
  public FileInfoBuilder withCreatingOsVersion(
      @JsonProperty("CreatingOsVersion") String creatingOsVersion) {
    this.creatingOsVersion = creatingOsVersion;
    return this;
  }

  /**
   * Spécifie la date de la dernière modification du fichier.
   *
   * @param lastModified la date de la dernière modification du fichier
   * @return le builder
   */
  public FileInfoBuilder withLastModified(
      @JsonProperty("LastModified") LocalDateTime lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  /**
   * Instancie la classe FileInfo selon les paramètres précédemment spécifiés dans le builder.
   *
   * @return le fileinfo
   */
  public FileInfo build() {
    return new FileInfo(
        filename,
        creatingApplicationName,
        creatingApplicationVersion,
        dateCreatedByApplication,
        creatingOs,
        creatingOsVersion,
        lastModified);
  }
}
