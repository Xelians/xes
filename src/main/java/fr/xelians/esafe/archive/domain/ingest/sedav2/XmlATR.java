/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import fr.xelians.esafe.archive.domain.atr.*;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.ByteArrayInOutStream;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import lombok.extern.slf4j.Slf4j;
import nu.xom.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class XmlATR {

  private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();
  private static final String UTF_8 = "UTF-8";

  private XmlATR() {}

  public static Reply createReply(Path atrPath)
      throws ParsingException, IOException, XMLStreamException {

    // We accept all namespaces
    String ns = Sedav2Utils.getArchiveTransferReplyNameSpace(atrPath);

    try (InputStream is = Files.newInputStream(atrPath)) {
      Element rootElem = new Builder().build(is).getRootElement();
      XPathContext xc = XPathContext.makeNamespaceContext(rootElem);

      if (!Sedav2Utils.NOT_FOUND.equals(ns)) xc.addNamespace("ns", ns);

      Nodes nodes1 = rootElem.query("//ns:MessageIdentifier", xc);
      if (nodes1.size() == 0)
        throw new BadRequestException(
            "Failed to process reply", "Message Identifier is not defined");

      Nodes nodes2 = rootElem.query("//ns:ReplyCode", xc);
      if (nodes2.size() == 0)
        throw new BadRequestException("Failed to process reply", "reply Code  is not defined");

      return new Reply(nodes1.get(0).getValue(), nodes2.get(0).getValue());
    }
  }

  public static InputStream createOkInputStream(ArchiveTransferReply atr)
      throws XMLStreamException {
    Validate.notNull(atr, Utils.NOT_NULL, "archiveTransferReply");

    XMLStreamWriter xmlWriter = null;
    ByteArrayInOutStream baios = new ByteArrayInOutStream(1024);

    try {
      xmlWriter = OUTPUT_FACTORY.createXMLStreamWriter(baios, UTF_8);

      xmlWriter.writeStartDocument("utf-8", "1.0");
      xmlWriter.writeStartElement("ArchiveTransferReply");
      xmlWriter.writeDefaultNamespace(Sedav2Utils.SEDA_V21);
      xmlWriter.writeNamespace("ns2", "http://www.w3.org/1999/xlink");
      xmlWriter.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      xmlWriter.writeAttribute(
          "xsi:schemaLocation",
          "fr:gouv:culture:archivesdefrance:seda:v2.1 seda/seda-2.1-main.xsd");

      xmlWriter.writeStartElement("Date");
      xmlWriter.writeCharacters(atr.getDate().toString());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("MessageIdentifier");
      xmlWriter.writeCharacters(atr.getMessageIdentifier());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("ArchivalAgreement");
      xmlWriter.writeCharacters(atr.getArchivalAgreement());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("CodeListVersions");
      xmlWriter.writeStartElement("ReplyCodeListVersion");
      xmlWriter.writeEndElement();
      xmlWriter.writeStartElement("MessageDigestAlgorithmCodeListVersion");
      xmlWriter.writeEndElement();
      xmlWriter.writeStartElement("FileFormatCodeListVersion");
      xmlWriter.writeEndElement();
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("DataObjectPackage");

      for (DataObjectGroupReply dog : atr.getDataObjectGroupReplys()) {
        xmlWriter.writeStartElement("DataObjectGroup");
        if (dog.getXmlId() != null) {
          xmlWriter.writeAttribute("id", dog.getXmlId());
        }

        for (BinaryDataObjectReply bdo : dog.getBinaryDataObjects()) {
          xmlWriter.writeStartElement("BinaryDataObject");
          if (StringUtils.isNotBlank(bdo.getXmlId())) {
            xmlWriter.writeAttribute("id", bdo.getXmlId());
          }

          xmlWriter.writeStartElement("DataObjectSystemId");
          xmlWriter.writeCharacters(bdo.getSystemId().toString());
          xmlWriter.writeEndElement();

          xmlWriter.writeStartElement("DataObjectGroupSystemId");
          xmlWriter.writeCharacters(bdo.getSystemId().toString());
          xmlWriter.writeEndElement();

          xmlWriter.writeStartElement("DataObjectVersion");
          xmlWriter.writeCharacters(bdo.getVersion());
          xmlWriter.writeEndElement();

          xmlWriter.writeEndElement();
        }

        for (PhysicalDataObjectReply pdo : dog.getPhysicalDataObjects()) {
          xmlWriter.writeStartElement("PhysicalDataObject");
          if (StringUtils.isNotBlank(pdo.getXmlId())) {
            xmlWriter.writeAttribute("id", pdo.getXmlId());
          }

          xmlWriter.writeStartElement("DataObjectSystemId");
          xmlWriter.writeCharacters(pdo.getSystemId());
          xmlWriter.writeEndElement();

          xmlWriter.writeStartElement("DataObjectGroupSystemId");
          xmlWriter.writeCharacters(pdo.getSystemId());
          xmlWriter.writeEndElement();

          xmlWriter.writeStartElement("DataObjectVersion");
          xmlWriter.writeCharacters(pdo.getVersion());
          xmlWriter.writeEndElement();

          xmlWriter.writeEndElement(); // PhysicalDataObject
        }
        xmlWriter.writeEndElement(); // DataObjectGroup
      }

      xmlWriter.writeStartElement("DescriptiveMetadata");

      for (ArchiveUnitReply unit : atr.getArchiveUnitReplys()) {
        xmlWriter.writeStartElement("ArchiveUnit");
        xmlWriter.writeAttribute("id", unit.getXmlId());

        xmlWriter.writeStartElement("Content");

        xmlWriter.writeStartElement("SystemId");
        xmlWriter.writeCharacters(unit.getSystemId());
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement(); // Content
        xmlWriter.writeEndElement(); // ArchiveUnit
      }
      xmlWriter.writeEndElement(); // DescriptiveMetadata

      xmlWriter.writeEmptyElement("ManagementMetadata");
      xmlWriter.writeEndElement(); // DataObjectPackage

      xmlWriter.writeStartElement("ReplyCode");
      xmlWriter.writeCharacters("OK");
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("MessageRequestIdentifier");
      xmlWriter.writeCharacters(atr.getMessageRequestIdentifier());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("GrantDate");
      xmlWriter.writeCharacters(atr.getGrantDate().toString());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("ArchivalAgency");
      xmlWriter.writeStartElement("Identifier");
      xmlWriter.writeCharacters(atr.getArchivalAgencyIdentifier());
      xmlWriter.writeEndElement();
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("TransferringAgency");
      xmlWriter.writeStartElement("Identifier");
      xmlWriter.writeCharacters(atr.getTransferringAgencyIdentifier());
      xmlWriter.writeEndElement();
      xmlWriter.writeEndElement();

      xmlWriter.writeEndElement();
      xmlWriter.writeEndDocument();
    } finally {
      if (xmlWriter != null) {
        xmlWriter.close();
      }
    }

    return baios.getInputStream();
  }

  public static InputStream createKoInputStream(OperationDb operationDb) throws XMLStreamException {
    Validate.notNull(operationDb, Utils.NOT_NULL, "operationDb");

    OperationType type = operationDb.getType();
    if (type != OperationType.INGEST_HOLDING
        && type != OperationType.INGEST_FILING
        && type != OperationType.INGEST_ARCHIVE) {
      throw new BadRequestException(
          "Failed to get archive transfer reply",
          String.format("No manifest available for operation id '%s'", operationDb.getId()));
    }

    OperationStatus status = operationDb.getStatus();
    if (status != OperationStatus.ERROR_INIT
        && status != OperationStatus.ERROR_CHECK
        && status != OperationStatus.ERROR_COMMIT) {
      throw new NotFoundException(
          "Failed to get manifest",
          String.format("No manifest available for Operation id %s", operationDb.getId()));
    }

    return generateXml(operationDb);
  }

  private static AtrInfo getAtrInfo(OperationDb operationDb) {
    String info = operationDb.getProperty01();
    try {
      return JsonService.to(info, AtrInfo.class);
    } catch (IOException e) {
      throw new InternalException(
          "Failed to get archive transfer reply",
          String.format("Error parsing operation id %s", operationDb.getId()),
          e);
    }
  }

  private static InputStream generateXml(OperationDb operationDb) throws XMLStreamException {
    Validate.notNull(operationDb, Utils.NOT_NULL, "operationDb");

    AtrInfo atrInfo = getAtrInfo(operationDb);

    XMLStreamWriter xmlWriter = null;
    ByteArrayInOutStream baios = new ByteArrayInOutStream(1024);

    try {
      xmlWriter = OUTPUT_FACTORY.createXMLStreamWriter(baios, UTF_8);

      xmlWriter.writeStartDocument("utf-8", "1.0");
      xmlWriter.writeStartElement("ArchiveTransferReply");
      xmlWriter.writeDefaultNamespace(Sedav2Utils.SEDA_V2);
      xmlWriter.writeNamespace("ns2", "http://www.w3.org/1999/xlink");
      xmlWriter.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      xmlWriter.writeAttribute(
          "xsi:schemaLocation",
          "fr:gouv:culture:archivesdefrance:seda:v2.1 seda/seda-2.1-main.xsd");

      xmlWriter.writeStartElement("Date");
      xmlWriter.writeCharacters(LocalDate.now().toString());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("MessageIdentifier");
      xmlWriter.writeCharacters(atrInfo.messageIdentifier());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("ArchivalAgreement");
      xmlWriter.writeCharacters(atrInfo.archivalAgreement());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("CodeListVersions");
      xmlWriter.writeStartElement("ReplyCodeListVersion");
      xmlWriter.writeEndElement(); // ReplyCodeListVersion
      xmlWriter.writeStartElement("MessageDigestAlgorithmCodeListVersion");
      xmlWriter.writeEndElement(); // MessageDigestAlgorithmCodeListVersion
      xmlWriter.writeStartElement("FileFormatCodeListVersion");
      xmlWriter.writeEndElement(); // FileFormatCodeListVersion
      xmlWriter.writeEndElement(); // CodeListVersions

      xmlWriter.writeStartElement("Operation");
      xmlWriter.writeStartElement("Event");
      xmlWriter.writeStartElement("EventTypeCode");
      xmlWriter.writeCharacters(operationDb.getType().toString());
      xmlWriter.writeEndElement(); // EventTypeCode
      xmlWriter.writeStartElement("EventType");
      xmlWriter.writeCharacters(operationDb.getTypeInfo());
      xmlWriter.writeEndElement(); // EventType
      xmlWriter.writeStartElement("EventDateTime");
      xmlWriter.writeCharacters(operationDb.getCreated().toString());
      xmlWriter.writeEndElement(); // EventDateTime
      xmlWriter.writeStartElement("Outcome");
      xmlWriter.writeCharacters(operationDb.getOutcome());
      xmlWriter.writeEndElement(); // Outcome
      xmlWriter.writeStartElement("OutcomeDetail");
      xmlWriter.writeCharacters(operationDb.getStatus().toString());
      xmlWriter.writeEndElement(); // OutcomeDetail
      xmlWriter.writeStartElement("OutcomeDetailMessage");
      xmlWriter.writeCharacters(operationDb.getMessage());
      xmlWriter.writeEndElement(); // OutcomeDetailMessage
      xmlWriter.writeEndElement(); // Event
      xmlWriter.writeEndElement(); // Operation

      xmlWriter.writeStartElement("ReplyCode");
      xmlWriter.writeCharacters("KO");
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("MessageRequestIdentifier");
      xmlWriter.writeCharacters(atrInfo.messageRequestIdentifier());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("GrantDate");
      xmlWriter.writeCharacters(operationDb.getCreated().toString());
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("ArchivalAgency");
      xmlWriter.writeStartElement("Identifier");
      xmlWriter.writeCharacters(atrInfo.archivalAgencyIdentifier());
      xmlWriter.writeEndElement();
      xmlWriter.writeEndElement();

      xmlWriter.writeStartElement("TransferringAgency");
      xmlWriter.writeStartElement("Identifier");
      xmlWriter.writeCharacters(atrInfo.transferringAgencyIdentifier());
      xmlWriter.writeEndElement();
      xmlWriter.writeEndElement();

      xmlWriter.writeEndElement();
      xmlWriter.writeEndDocument();
    } finally {
      if (xmlWriter != null) {
        xmlWriter.close();
      }
    }

    return baios.getInputStream();
  }
}
