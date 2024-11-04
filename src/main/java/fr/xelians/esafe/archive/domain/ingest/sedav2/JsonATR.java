/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import com.fasterxml.jackson.core.JsonGenerator;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.unit.ArchiveTransfer;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.ManagementMetadata;
import fr.xelians.esafe.archive.domain.unit.object.*;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.common.utils.SipUtils;
import fr.xelians.esafe.common.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class JsonATR {

  private JsonATR() {}

  public static byte[] toBytes(
      ArchiveTransfer archiveTransfer,
      ManagementMetadata managementMetadata,
      List<DataObjectGroup> dataObjectGroups,
      List<ArchiveUnit> archiveUnits)
      throws XMLStreamException, IOException {

    Validate.notNull(archiveTransfer, Utils.NOT_NULL, "archiveTransfer");
    Validate.notNull(dataObjectGroups, Utils.NOT_NULL, "dataObjectGroups");
    Validate.notNull(archiveUnits, Utils.NOT_NULL, "archiveUnits");

    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    try (JsonGenerator generator = JsonService.createGenerator(baos, JsonConfig.DEFAULT)) {

      generator.writeStartObject();
      generator.writeStringField("Type", ReportType.INGEST.toString());
      generator.writeStringField("Date", LocalDateTime.now().toString());

      String msgId =
          Objects.toString(
              archiveTransfer.getMessageIdentifier(), SipUtils.randomMessageIdentifier());
      generator.writeStringField("MessageIdentifier", msgId);
      generator.writeStringField("MessageRequestIdentifier", msgId);

      generator.writeNumberField("Tenant", archiveTransfer.getTenant());
      generator.writeStringField("OperationId", archiveTransfer.getOperationId().toString());
      generator.writeStringField("ArchivalAgreement", archiveTransfer.getArchivalAgreement());
      generator.writeStringField(
          "ArchivalAgencyIdentifier", archiveTransfer.getArchivalAgency().identifier());
      generator.writeStringField(
          "TransferringAgencyIdentifier", archiveTransfer.getTransferringAgency().identifier());

      if (managementMetadata != null) {
        if (managementMetadata.getArchivalProfile() != null) {
          generator.writeStringField("ArchivalProfile", managementMetadata.getArchivalProfile());
        }
        if (managementMetadata.getAcquisitionInformation() != null) {
          generator.writeStringField(
              "AcquisitionInformation", managementMetadata.getAcquisitionInformation());
        }
        if (managementMetadata.getServiceLevel() != null) {
          generator.writeStringField("ServiceLevel", managementMetadata.getServiceLevel());
        }
        if (managementMetadata.getLegalStatus() != null) {
          generator.writeStringField("LegalStatus", managementMetadata.getLegalStatus());
        }
      }

      generator.writeStringField("GrantDate", archiveTransfer.getCreated().toString());
      generator.writeStringField("ReplyCode", "OK");

      int numOfUnits = 0;
      int numOfObjectGroups = 0;
      int numOfPhysicalObjects = 0;
      int numOfBinaryObjects = 0;
      long sizeOfBinaryObjects = 0;

      generator.writeFieldName("DataObjectGroups");
      generator.writeStartArray();
      for (DataObjectGroup dog : dataObjectGroups) {
        numOfObjectGroups++;
        generator.writeStartObject();
        generator.writeStringField("XmlId", Objects.toString(dog.getXmlId(), ""));

        generator.writeFieldName("BinaryDataObjects");
        generator.writeStartArray();
        for (BinaryDataObject bdo : dog.getBinaryDataObjects()) {
          numOfBinaryObjects++;
          sizeOfBinaryObjects += bdo.getSize();
          generator.writeStartObject();
          generator.writeStringField("XmlId", Objects.toString(bdo.getXmlId(), ""));
          generator.writeStringField("DataObjectSystemId", bdo.getId().toString());
          generator.writeStringField("DataObjectVersion", bdo.getBinaryVersion());
          generator.writeNumberField("Size", bdo.getSize());
          generator.writeStringField("DigestAlgorithm", bdo.getDigestAlgorithm());
          generator.writeStringField("MessageDigest", bdo.getMessageDigest());
          generator.writeEndObject();
        }
        generator.writeEndArray();

        generator.writeFieldName("PhysicalDataObjects");
        generator.writeStartArray();
        for (PhysicalDataObject pdo : dog.getPhysicalDataObjects()) {
          numOfPhysicalObjects++;
          generator.writeStartObject();
          generator.writeStringField("XmlId", Objects.toString(pdo.getXmlId(), ""));
          generator.writeStringField("DataObjectSystemId", pdo.getPhysicalId());
          generator.writeStringField(
              "DataObjectVersion", Objects.toString(pdo.getPhysicalVersion(), ""));
          generator.writeEndObject();
        }
        generator.writeEndArray();

        generator.writeEndObject();
      }
      generator.writeEndArray();

      generator.writeFieldName("ArchiveUnits");
      generator.writeStartArray();
      for (ArchiveUnit unit : archiveUnits) {
        numOfUnits++;
        generator.writeStartObject();
        generator.writeStringField("XmlId", Objects.toString(unit.getXmlId(), ""));
        generator.writeStringField("SystemId", unit.getId().toString());
        JsonUtils.writeStringsField(
            generator, "OriginatingSystemIds", unit.getOriginatingSystemIds());
        JsonUtils.writeStringsField(
            generator, "ArchivalAgencyIdentifiers", unit.getServiceProducers());

        int numOfUnitPhysicalObjects = 0;
        int numOfUnitBinaryObjects = 0;
        long sizeOfUnitBinaryObjects = 0;

        for (Qualifiers qualifier : unit.getQualifiers()) {
          if (qualifier.isBinaryQualifier()) {
            numOfUnitBinaryObjects += qualifier.getNbc();
            for (ObjectVersion ov : qualifier.getVersions()) {
              sizeOfUnitBinaryObjects += ov.getSize();
            }
          } else {
            numOfUnitPhysicalObjects += qualifier.getNbc();
          }
        }

        generator.writeNumberField("NumOfPhysicalObjects", numOfUnitPhysicalObjects);
        generator.writeNumberField("NumOfBinaryObjects", numOfUnitBinaryObjects);
        generator.writeNumberField("SizeOfBinaryObjects", sizeOfUnitBinaryObjects);
        generator.writeEndObject();
      }
      generator.writeEndArray();

      generator.writeNumberField("NumOfUnits", numOfUnits);
      generator.writeNumberField("NumOfObjectGroups", numOfObjectGroups);
      generator.writeNumberField("NumOfPhysicalObjects", numOfPhysicalObjects);
      generator.writeNumberField("NumOfBinaryObjects", numOfBinaryObjects);
      generator.writeNumberField("SizeOfBinaryObjects", sizeOfBinaryObjects);

      generator.writeEndObject();
    }

    return baos.toByteArray();
  }
}
