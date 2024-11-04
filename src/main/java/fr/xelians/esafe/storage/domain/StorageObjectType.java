/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import lombok.Getter;

/*
 * @author Emmanuel Deviller
 */
@Getter
public enum StorageObjectType {
  ope("operation"), // OPERATION (immutable)
  mft("manifest"), // MANIFEST  (immutable)
  bin("binary"), // PACKED BINARY OBJECT (immutable)
  atr("atr"), // ATR (immutable)
  dip("dip"), // Dissemination Package (immutable)
  aus("tmp"), // Temporary Units (immutable)
  uni("units"), // Units (mutable)
  rep("report"), // Report (immutable)

  lbk("logbook"), // Logbook Storage (immutable, écrit par le batch de sécurisation)

  age("agency"), // Agency (mutable)
  rul("rule"), // Rule (mutable)
  pro("profile"), // Profile (mutable)
  acc("accesscontract"), // Access Contract (mutable)
  ing("ingestcontract"), // Ingest Contract (mutable)
  ind("ontology"), // Ontology (mutable)

  ten("tenant"), // Tenant (mutable)
  org("organization"), // Organization (mutable)
  usr("user"), // User (mutable)
  gra("grant"); // Grant (mutable)

  private final String desc;

  StorageObjectType(String desc) {
    this.desc = desc;
  }
}
