/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import com.fasterxml.jackson.core.JsonGenerator;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.object.BinaryDataObject;
import fr.xelians.esafe.archive.domain.unit.object.DataObjectGroup;
import fr.xelians.esafe.archive.domain.unit.object.PhysicalDataObject;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;

@Slf4j
public class JsonATR {

  private JsonATR() {}

  public static byte[] toBytes(
      ArchiveTransfer archiveTransfer,
      List<DataObjectGroup> dataObjectGroups,
      List<ArchiveUnit> archiveUnits)
      throws XMLStreamException, IOException {

    Validate.notNull(archiveTransfer, Utils.NOT_NULL, "archiveTransfer");
    Validate.notNull(dataObjectGroups, Utils.NOT_NULL, "dataObjectGroups");
    Validate.notNull(archiveUnits, Utils.NOT_NULL, "archiveUnits");

    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    try (JsonGenerator generator = JsonService.createGenerator(baos, JsonConfig.DEFAULT)) {

      generator.writeStartObject();
      generator.writeStringField("Date", LocalDateTime.now().toString());
      generator.writeStringField(
          "MessageIdentifier", RandomStringUtils.randomAlphabetic(32).toLowerCase());
      generator.writeStringField("ArchivalAgreement", archiveTransfer.getArchivalAgreement());

      generator.writeFieldName("DataObjectGroups");
      generator.writeStartArray();
      for (DataObjectGroup dog : dataObjectGroups) {
        generator.writeStartObject();
        generator.writeStringField("XmlId", Objects.toString(dog.getXmlId(), ""));

        generator.writeFieldName("BinaryDataObjects");
        generator.writeStartArray();
        for (BinaryDataObject bdo : dog.getBinaryDataObjects()) {
          generator.writeStartObject();
          generator.writeStringField("XmlId", Objects.toString(bdo.getXmlId(), ""));
          generator.writeStringField("DataObjectSystemId", String.valueOf(bdo.getId()));
          generator.writeStringField("DataObjectVersion", bdo.getBinaryVersion());
          generator.writeStringField("DigestAlgorithm", bdo.getDigestAlgorithm());
          generator.writeStringField("MessageDigest", bdo.getMessageDigest());
          generator.writeEndObject();
        }
        generator.writeEndArray();

        generator.writeFieldName("PhysicalDataObjects");
        generator.writeStartArray();
        for (PhysicalDataObject pdo : dog.getPhysicalDataObjects()) {
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
        generator.writeStartObject();
        generator.writeStringField("XmlId", Objects.toString(unit.getXmlId(), ""));
        generator.writeStringField("SystemId", unit.getId().toString());
        generator.writeStringField(
            "OriginatingSystemId", Objects.toString(unit.getOriginatingSystemIds(), ""));
        generator.writeEndObject();
      }
      generator.writeEndArray();

      generator.writeStringField("ReplyCode", "OK");
      generator.writeStringField(
          "MessageRequestIdentifier", Objects.toString(archiveTransfer.getMessageIdentifier(), ""));
      generator.writeStringField("GrantDate", archiveTransfer.getCreated().toString());
      generator.writeStringField(
          "ArchivalAgencyIdentifier", archiveTransfer.getArchivalAgency().identifier());
      generator.writeStringField(
          "TransferringAgencyIdentifier", archiveTransfer.getTransferringAgency().identifier());

      generator.writeEndObject();
    }

    return baos.toByteArray();
  }
}
