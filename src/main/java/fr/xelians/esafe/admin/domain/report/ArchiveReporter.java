/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.core.JsonGenerator;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
public class ArchiveReporter implements AutoCloseable {

  private int numOfUnits = 0;
  private int numOfObjectGroups = 0;
  private int numOfPhysicalObjects = 0;
  private int numOfBinaryObjects = 0;
  private long sizeOfBinaryObjects = 0;

  private final JsonGenerator generator;

  public ArchiveReporter(ReportType type, ReportStatus status, OperationDb operation, Path path)
      throws IOException {

    OutputStream os = Files.newOutputStream(path);
    generator = JsonService.createGenerator(os);

    generator.writeStartObject();
    generator.writeStringField("Type", type.toString());
    generator.writeStringField("Date", LocalDateTime.now().toString());
    generator.writeNumberField("Tenant", operation.getTenant());
    generator.writeNumberField("OperationId", operation.getId());
    generator.writeStringField("GrantDate", operation.getCreated().toString());
    generator.writeStringField("Status", status.toString());

    generator.writeFieldName("ArchiveUnits");
    generator.writeStartArray();
  }

  private void doWriteUnit(ArchiveUnit au) throws IOException {
    numOfUnits++;

    generator.writeStartObject();
    generator.writeStringField("SystemId", au.getId().toString());
    generator.writeNumberField("OperationId", au.getOperationId());
    generator.writeStringField("ArchivalAgencyIdentifier", au.getServiceProducer());
    JsonUtils.writeStringsField(generator, "ArchivalAgencyIdentifiers", au.getServiceProducers());
    generator.writeStringField("CreationDate", au.getCreationDate().toString());
  }

  public void writeUnit(ArchiveUnit au) throws IOException {
    doWriteUnit(au);

    boolean isNotEmpty = false;

    for (Qualifiers qualifiers : au.getQualifiers()) {
      if (qualifiers.isBinaryQualifier()) {
        for (ObjectVersion ov : qualifiers.getVersions()) {
          isNotEmpty = true;
          numOfBinaryObjects++;
          sizeOfBinaryObjects += ov.getSize();
        }
      }
    }

    for (Qualifiers qualifiers : au.getQualifiers()) {
      if (qualifiers.isPhysicalQualifier()) {
        for (ObjectVersion ov : qualifiers.getVersions()) {
          isNotEmpty = true;
          numOfPhysicalObjects++;
        }
      }
    }

    if (isNotEmpty) {
      numOfObjectGroups++;
    }

    generator.writeEndObject();
  }

  public void writeUnitWithObjects(ArchiveUnit au) throws IOException {
    doWriteUnit(au);

    boolean isNotEmpty = false;

    generator.writeFieldName("BinaryDataObjects");
    generator.writeStartArray();
    for (Qualifiers qualifiers : au.getQualifiers()) {
      if (qualifiers.isBinaryQualifier()) {
        for (ObjectVersion ov : qualifiers.getVersions()) {
          isNotEmpty = true;
          numOfBinaryObjects++;
          sizeOfBinaryObjects += ov.getSize();
          generator.writeStartObject();
          generator.writeStringField("DataObjectSystemId", ov.getId().toString());
          generator.writeStringField("DataObjectVersion", ov.getDataObjectVersion());
          generator.writeNumberField("Size", ov.getSize());
          generator.writeStringField("DigestAlgorithm", ov.getAlgorithm());
          generator.writeStringField("MessageDigest", ov.getMessageDigest());
          generator.writeEndObject();
        }
      }
    }
    generator.writeEndArray();

    generator.writeFieldName("PhysicalDataObjects");
    generator.writeStartArray();
    for (Qualifiers qualifiers : au.getQualifiers()) {
      if (qualifiers.isPhysicalQualifier()) {
        for (ObjectVersion ov : qualifiers.getVersions()) {
          isNotEmpty = true;
          numOfPhysicalObjects++;
          generator.writeStartObject();
          generator.writeStringField("DataObjectSystemId", ov.getId().toString());
          generator.writeStringField("DataObjectVersion", ov.getDataObjectVersion());
          generator.writeEndObject();
        }
      }
    }

    if (isNotEmpty) {
      numOfObjectGroups++;
    }

    generator.writeEndArray();
    generator.writeEndObject();
  }

  @Override
  public void close() throws IOException {
    generator.writeEndArray();

    generator.writeNumberField("NumOfUnits", numOfUnits);
    generator.writeNumberField("NumOfObjectGroups", numOfObjectGroups);
    generator.writeNumberField("NumOfPhysicalObjects", numOfPhysicalObjects);
    generator.writeNumberField("NumOfBinaryObjects", numOfBinaryObjects);
    generator.writeNumberField("SizeOfBinaryObjects", sizeOfBinaryObjects);
    generator.writeEndObject();

    generator.close();
  }
}
