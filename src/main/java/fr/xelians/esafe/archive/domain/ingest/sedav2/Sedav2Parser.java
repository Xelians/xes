/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import static fr.xelians.esafe.common.constant.Sedav2.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.ingest.AbstractManifestParser;
import fr.xelians.esafe.archive.domain.ingest.OntologyMap;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.object.*;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.archive.domain.unit.rules.computed.ComputedInheritedRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
import fr.xelians.esafe.archive.service.DateRuleService;
import fr.xelians.esafe.archive.service.IngestService;
import fr.xelians.esafe.common.exception.functional.ManifestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.DateUtils;
import fr.xelians.esafe.common.utils.SipUtils;
import fr.xelians.esafe.common.utils.UnitUtils;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.sequence.Sequence;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class Sedav2Parser extends AbstractManifestParser {

  private static final XMLInputFactory INPUT_FACTORY = Sedav2Utils.getInputFactory();
  private static final QName XML_ID = new QName("id");

  public static final String CLOSE_ARCHIVE_TRANSFER_FAILED = "Close archive transfer failed";
  public static final String READ_ARCHIVE_TRANSFER_FAILED = "Read archive transfer failed";
  public static final String READ_DATA_OBJECT_REFERENCE_FAILED =
      "Read data object reference failed";
  public static final String READ_PHYSICAL_OBJECT_FAILED = "Read physical object failed";
  public static final String PHYSICAL_OBJECT_MUST_HAVE_NON_NULL_UNIQUE_ID_ATTRIBUTE =
      "Physical object must have non null unique id attribute";
  public static final String READ_BINARY_OBJECT_FAILED = "Read binary object failed";
  public static final String BINARY_OBJECT_MUST_HAVE_NON_NULL_UNIQUE_ID_ATTRIBUTE =
      "Binary object must have non null unique id attribute";
  public static final String READ_HOLD_RULE_FAILED = "Read hold rule failed";
  public static final String RULE_NAME_AFTER_START_DATE =
      "Rule name must be declared before start date";

  public static final String IMPLEMENTATION_VERSION = "5.2";

  private final ArchiveTransfer archiveTransfer = new ArchiveTransfer();
  private final List<ArchiveUnit> referrerUnits = new ArrayList<>();
  private final List<ArchiveUnit> archiveUnits = new ArrayList<>();
  private final Map<Long, ArchiveUnit> rootUnits = new HashMap<>();
  private final Map<String, ArchiveUnit> archiveUnitMap = new HashMap<>();
  private final Map<String, DataObjectGroup> dataObjectGroupMap = new HashMap<>();

  private String sedaVersion;
  private Path manifestPath;
  private Path sipDir;
  private Sequence sequence;
  private UnitType unitType;
  private ManagementMetadata managementMetadata;

  public Sedav2Parser(IngestService ingestService, OperationDb operation) {
    super(ingestService, operation);
    archiveTransfer.setTenant(operation.getTenant());
    archiveTransfer.setOperationId(operation.getId());
    archiveTransfer.setCreated(operation.getCreated());
  }

  private static UnitType getUnitType(OperationType operationType) {
    return switch (operationType) {
      case INGEST_ARCHIVE -> UnitType.INGEST;
      case INGEST_FILING -> UnitType.FILING_UNIT;
      case INGEST_HOLDING -> UnitType.HOLDING_UNIT;
      default -> throw new InternalException(
          "Get unit type failed", String.format("Bad operation type %s", operationType));
    };
  }

  @Override
  public void parse(String sedaVersion, Path manifestPath, Path sipDir) throws IOException {
    this.sedaVersion = sedaVersion;
    this.manifestPath = manifestPath;
    this.sipDir = sipDir;
    this.sequence = sequenceService.createSequence();
    this.unitType = getUnitType(operationType);

    try (InputStream is = Files.newInputStream(manifestPath)) {
      XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(is);
      readArchiveTransfer(reader);
    } catch (XMLStreamException ex) {
      throw new ManifestException("Failed to parse manifest", "", ex);
    }
  }

  @Override
  public ArchiveTransfer getArchiveTransfer() {
    return archiveTransfer;
  }

  @Override
  public List<DataObjectGroup> getDataObjectGroups() {
    return new ArrayList<>(dataObjectGroupMap.values());
  }

  @Override
  public List<ArchiveUnit> getArchiveUnits() {
    return archiveUnits;
  }

  @Override
  public ManagementMetadata getManagementMetadata() {
    return managementMetadata;
  }

  private void readArchiveTransfer(XMLEventReader reader) throws XMLStreamException, IOException {

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case MESSAGE_IDENTIFIER -> archiveTransfer.setMessageIdentifier(readText(reader, 1024));
          case COMMENT ->
          // Note. We don't support multiple comments. We only take the last one.
          archiveTransfer.setComment(readText(reader, 4096));
          case ARCHIVAL_AGREEMENT -> {
            String agreement = readText(reader, 512);
            checkArchivalAgreement(agreement);
            archiveTransfer.setArchivalAgreement(agreement);
          }
          case DATA_OBJECT_GROUP -> {
            assertTrue(
                operationType == OperationType.INGEST_ARCHIVE,
                READ_ARCHIVE_TRANSFER_FAILED,
                "Data object group is not allowed in filing and holding plans");
            readDataObjectGroup(reader, startElement);
          }
          case BINARY_DATA_OBJECT -> {
            // Old SEDA structure - do not use (implemented for compatibility)
            assertTrue(
                operationType == OperationType.INGEST_ARCHIVE,
                READ_ARCHIVE_TRANSFER_FAILED,
                "Binary data object is not allowed in filing and holding plans");
            readBinaryObject(reader, startElement, null);
          }
          case PHYSICAL_DATA_OBJECT -> {
            // Old SEDA structure - do not use (implemented for compatibility)
            assertTrue(
                operationType == OperationType.INGEST_ARCHIVE,
                READ_ARCHIVE_TRANSFER_FAILED,
                "Physical data object is not allowed in filing and holding plans");
            readPhysicalObject(reader, startElement, null);
          }
          case ARCHIVE_UNIT -> readArchiveUnit(reader, startElement, null);
          case MANAGEMENT_METADATA -> readManagementMetadata(reader);
          case ARCHIVAL_AGENCY -> {
            // This is the archive producer if the originating agency is
            // not defined in management metadata
            Agency sp = getAgency(reader);
            checkAgency(sp.identifier());
            archiveTransfer.setArchivalAgency(sp);
          }
          case TRANSFERRING_AGENCY -> {
            Agency sv = getAgency(reader);
            checkAgency(sv.identifier()); // is it necessary ?
            archiveTransfer.setTransferringAgency(sv);
          }
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals("ArchiveTransfer")) {
          closeArchiveTransfer();
          break;
        }
      }
    }
  }

  private void closeArchiveTransfer() throws IOException {

    assertNotNull(
        archiveTransfer.getArchivalAgency(),
        CLOSE_ARCHIVE_TRANSFER_FAILED,
        "Archival agency is not defined in archive transfer");

    // Process all referrers units (that reference itself by xml id reference)
    for (ArchiveUnit unit : referrerUnits) {
      String xmlIdRef = unit.getArchiveUnitRefId();
      // Get the referenced unit
      ArchiveUnit unitRef = archiveUnitMap.get(xmlIdRef);

      assertNotNull(
          unitRef,
          CLOSE_ARCHIVE_TRANSFER_FAILED,
          String.format("Unable to find archive unit '%s' in manifest", xmlIdRef));

      assertTrue(
          unitRef.isDetached(),
          CLOSE_ARCHIVE_TRANSFER_FAILED,
          String.format("Unit '%s' is already attached in manifest", xmlIdRef));

      // Attach the referenced unit to its real parent (i.e. the referrer's parent)
      unitRef.setParentUnit(unit.getParentUnit());
      unitRef.setParentId(unit.getParentId());

      // The referer unit can now be detached fom its parent and removed from the archives map
      unit.removeParentUnit();
      archiveUnitMap.remove(unit.getXmlId());
    }

    ArchiveUnit parentUnit = null;

    // Attach the remaining unattached Archive Units (i.e. units without parent)
    for (ArchiveUnit unit : archiveUnitMap.values()) {
      if (unit.isDetached()) {
        if (parentUnit == null) {
          parentUnit = getParentUnit(getAgreementLinkId());
        }
        unit.setParentUnit(parentUnit);
        unit.setParentId(parentUnit.getId());
      }
    }

    // Set Service Providers and ParentIds
    String sp = getOriginatingAgencyIdentifier();
    for (ArchiveUnit rootUnit : rootUnits.values()) {
      Set<String> rootSps = rootUnit.getServiceProducers();
      visitArchiveUnits(rootUnit, sp, rootSps);
    }
  }

  private void visitArchiveUnits(ArchiveUnit parentUnit, String sp, Set<String> rootSps) {
    for (ArchiveUnit unit : parentUnit.getChildUnitMap().values()) {
      unit.addToParentIds(parentUnit.getId());
      unit.addToParentIds(parentUnit.getParentIds());
      if (operationType != OperationType.INGEST_HOLDING) {
        unit.setServiceProducer(sp);
        unit.addToServiceProviders(sp);
        unit.addToServiceProviders(rootSps);

        // Compute MaxEndDate with inherited rules
        ComputedInheritedRules parentCir = parentUnit.getComputedInheritedRules();
        if (parentCir != null) {
          DateRuleService.computeMaxEndDates(unit, parentCir);
        }
      }
      unit.buildProperties(ontologyMapper);
      archiveUnits.add(unit);
      visitArchiveUnits(unit, sp, rootSps);
    }
  }

  private @NotNull ArchiveUnit getParentUnit(Long parentId) throws IOException {
    ArchiveUnit parentUnit = getLinkUnit(parentId);
    checkAttachedUnitType(unitType, parentUnit.getUnitType());
    rootUnits.put(parentUnit.getId(), parentUnit);
    return parentUnit;
  }

  private String getOriginatingAgencyIdentifier() {
    if (managementMetadata != null
        && StringUtils.isNotBlank(managementMetadata.getOriginatingAgencyIdentifier())) {
      return managementMetadata.getOriginatingAgencyIdentifier();
    }
    return archiveTransfer.getArchivalAgency().identifier();
  }

  private Agency getAgency(XMLEventReader reader) throws XMLStreamException {
    String identifier = null;
    String name = null;

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        String localPart = startElement.getName().getLocalPart();
        if (IDENTIFIER.equals(localPart)) {
          identifier = readText(reader, 512);
        } else if (NAME.equals(localPart)) {
          name = readText(reader, 512);
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        String tagName = endElement.getName().getLocalPart();
        if (ARCHIVAL_AGENCY.equals(tagName)
            || TRANSFERRING_AGENCY.equals(tagName)
            || SUBMISSION_AGENCY.equals(tagName)
            || ORIGINATING_AGENCY.equals(tagName)) {
          break;
        }
      }
    }
    return new Agency(identifier, name);
  }

  private void readManagementMetadata(XMLEventReader reader) throws XMLStreamException {
    managementMetadata = new ManagementMetadata();

    String archivalProfile = null;

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        String localPart = startElement.getName().getLocalPart();

        switch (localPart) {
          case ORIGINATING_AGENCY_IDENTIFIER -> {
            // This is the archive producer
            String agencyIdentifier = readText(reader, 512);
            checkAgency(agencyIdentifier);
            managementMetadata.setOriginatingAgencyIdentifier(agencyIdentifier);
          }
          case SUBMISSION_AGENCY_IDENTIFIER -> {
            String agencyIdentifier = readText(reader, 512);
            checkAgency(agencyIdentifier);
            managementMetadata.setSubmissionAgencyIdentifier(agencyIdentifier);
          }
          case ARCHIVAL_PROFILE -> {
            archivalProfile = readText(reader, 512);
            managementMetadata.setArchivalProfile(archivalProfile);
          }
          case ACQUISITION_INFORMATION -> {
            String acquisitionInformation = readText(reader, 512);
            managementMetadata.setAcquisitionInformation(acquisitionInformation);
          }
          case SERVICE_LEVEL -> {
            String serviceLevel = readText(reader, 512);
            managementMetadata.setServiceLevel(serviceLevel);
          }
          case LEGAL_STATUS -> {
            String legalStatus = readText(reader, 512);
            managementMetadata.setLegalStatus(legalStatus);
          }
        }

      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(MANAGEMENT_METADATA)) {
          checkArchivalProfile(archivalProfile, manifestPath);
          break;
        }
      }
    }
  }

  private void readDataObjectGroup(XMLEventReader reader, StartElement element)
      throws XMLStreamException, IOException {
    DataObjectGroup dataObjectGroup = new DataObjectGroup();
    Attribute attr = element.getAttributeByName(XML_ID);
    assertFalse(
        attr == null || StringUtils.isBlank(attr.getValue()),
        "Read data object group failed",
        "Data object group must have non null unique id attribute");

    String xmlId = attr.getValue();
    dataObjectGroup.setXmlId(xmlId);
    dataObjectGroupMap.put(xmlId, dataObjectGroup);

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        String localPart = startElement.getName().getLocalPart();
        if (localPart.equals(PHYSICAL_DATA_OBJECT)) {
          readPhysicalObject(reader, startElement, dataObjectGroup);
        } else if (localPart.equals(BINARY_DATA_OBJECT)) {
          readBinaryObject(reader, startElement, dataObjectGroup);
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(DATA_OBJECT_GROUP)) {
          break;
        }
      }
    }
  }

  // Note. dataObjectGroup may be null in the old Seda structure
  private void readPhysicalObject(
      XMLEventReader reader, StartElement element, DataObjectGroup dataObjectGroup)
      throws XMLStreamException {

    PhysicalDataObject physicalDataObject = new PhysicalDataObject();
    physicalDataObject.setId(sequence.nextValue());
    Attribute attr = element.getAttributeByName(XML_ID);
    if (attr != null && StringUtils.isNotBlank(attr.getValue())) {
      physicalDataObject.setXmlId(attr.getValue());
    }
    if (dataObjectGroup != null) {
      dataObjectGroup.addPhysicalDataObject(physicalDataObject);
    }

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case DATA_OBJECT_GROUP_ID -> {
            if (dataObjectGroup == null) {
              String dogi = readText(reader, 512);
              dataObjectGroup =
                  dataObjectGroupMap.computeIfAbsent(dogi, k -> new DataObjectGroup());
              dataObjectGroup.addPhysicalDataObject(physicalDataObject);
            }
          }
          case PHYSICAL_VERSION -> {
            dataObjectGroup = createDataObjectGroup(dataObjectGroup, physicalDataObject);
            String version = readText(reader, 512);
            checkPhysicalVersion(version, dataObjectGroup.getPhysicalDataObjects());
            physicalDataObject.setPhysicalVersion(version);
          }
          case PHYSICAL_ID -> {
            dataObjectGroup = createDataObjectGroup(dataObjectGroup, physicalDataObject);
            // This is the responsibility of the producer to ensure unique physical id
            physicalDataObject.setPhysicalId(readText(reader, 512));
          }
          case MEASURE -> {
            dataObjectGroup = createDataObjectGroup(dataObjectGroup, physicalDataObject);
            physicalDataObject.setMeasure(readDouble(reader));
          }
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(PHYSICAL_DATA_OBJECT)) {
          if (physicalDataObject.getPhysicalVersion() == null) {
            physicalDataObject.setPhysicalVersion(SipUtils.PHYSICAL_MASTER + "_1");
          }
          break;
        }
      }
    }
  }

  private DataObjectGroup createDataObjectGroup(
      DataObjectGroup dataObjectGroup, PhysicalDataObject physicalDataObject) {
    if (dataObjectGroup == null) {
      assertFalse(
          StringUtils.isBlank(physicalDataObject.getXmlId()),
          READ_PHYSICAL_OBJECT_FAILED,
          PHYSICAL_OBJECT_MUST_HAVE_NON_NULL_UNIQUE_ID_ATTRIBUTE);
      dataObjectGroup =
          dataObjectGroupMap.computeIfAbsent(
              physicalDataObject.getXmlId(), k -> new DataObjectGroup());
      dataObjectGroup.addPhysicalDataObject(physicalDataObject);
    }
    return dataObjectGroup;
  }

  private void readBinaryObject(
      XMLEventReader reader, StartElement element, DataObjectGroup dataObjectGroup)
      throws XMLStreamException, IOException {
    BinaryDataObject binaryDataObject = new BinaryDataObject();
    binaryDataObject.setId(sequence.nextValue());

    Attribute attr = element.getAttributeByName(XML_ID);
    if (attr != null && StringUtils.isNotBlank(attr.getValue())) {
      binaryDataObject.setXmlId(attr.getValue());
    }
    if (dataObjectGroup != null) {
      dataObjectGroup.addBinaryDataObject(binaryDataObject);
    }

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case DATA_OBJECT_GROUP_ID -> {
            if (dataObjectGroup == null) {
              String dogi = readText(reader, 512);
              dataObjectGroup =
                  dataObjectGroupMap.computeIfAbsent(dogi, k -> new DataObjectGroup());
              dataObjectGroup.addBinaryDataObject(binaryDataObject);
            }
          }
          case DATA_OBJECT_VERSION -> {
            dataObjectGroup = createDataObjectGroup(dataObjectGroup, binaryDataObject);
            String version =
                checkBinaryVersion(readText(reader, 512), dataObjectGroup.getBinaryDataObjects());
            binaryDataObject.setBinaryVersion(version);
          }
          case URI -> {
            dataObjectGroup = createDataObjectGroup(dataObjectGroup, binaryDataObject);
            binaryDataObject.setBinaryPath(sipDir.resolve(readText(reader, 1024)));
          }
          case MESSAGE_DIGEST -> {
            Attribute attribute = startElement.getAttributeByName(new QName("algorithm"));
            binaryDataObject.setDigestAlgorithm(attribute.getValue());
            binaryDataObject.setMessageDigest(readText(reader, 1024));
          }
          case SIZE -> {
            long size = Long.parseLong(readText(reader, 512));
            binaryDataObject.setSize(size);
          }
          case FORMAT_IDENTIFICATION -> readFormatIdentification(reader, binaryDataObject);
          case FILE_INFO -> readFileInfo(reader, binaryDataObject);
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(BINARY_DATA_OBJECT)) {
          checkBinary(binaryDataObject.getBinaryPath());

          if (binaryDataObject.getFormatIdentification() == null) {
            binaryDataObject.setFormatIdentification(new FormatIdentification());
          }
          checkBinaryFormat(
              binaryDataObject.getBinaryPath(),
              binaryDataObject.getFormatIdentification(),
              binaryDataObject.getFileInfo());

          if (binaryDataObject.getBinaryVersion() == null) {
            // We set a default binary version if not specified
            binaryDataObject.setBinaryVersion(BinaryQualifier.BinaryMaster + "_1");
          }

          if (binaryDataObject.getSize() == 0) {
            binaryDataObject.setSize(Files.size(binaryDataObject.getBinaryPath()));
          } else {
            long size = Files.size(binaryDataObject.getBinaryPath());
            binaryDataObject.setSize(checkBinarySize(size, binaryDataObject.getSize()));
          }

          checkBinaryDigest(
              binaryDataObject.getBinaryPath(),
              binaryDataObject.getDigestAlgorithm(),
              binaryDataObject.getMessageDigest());
          break;
        }
      }
    }
  }

  private DataObjectGroup createDataObjectGroup(
      DataObjectGroup dataObjectGroup, BinaryDataObject binaryDataObject) {
    if (dataObjectGroup == null) {
      assertFalse(
          StringUtils.isBlank(binaryDataObject.getXmlId()),
          READ_BINARY_OBJECT_FAILED,
          BINARY_OBJECT_MUST_HAVE_NON_NULL_UNIQUE_ID_ATTRIBUTE);
      dataObjectGroup =
          dataObjectGroupMap.computeIfAbsent(
              binaryDataObject.getXmlId(), k -> new DataObjectGroup());
      dataObjectGroup.addBinaryDataObject(binaryDataObject);
    }
    return dataObjectGroup;
  }

  private void readFormatIdentification(XMLEventReader reader, BinaryDataObject binaryDataObject)
      throws XMLStreamException {
    FormatIdentification formatIdentification = new FormatIdentification();
    binaryDataObject.setFormatIdentification(formatIdentification);

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case FORMAT_ID -> formatIdentification.setFormatId(readText(reader, 512));
          case FORMAT_LITTERAL -> formatIdentification.setFormatLitteral(readText(reader, 512));
          case FORMAT_NAME -> formatIdentification.setFormatName(readText(reader, 512));
          case MIME_TYPE -> formatIdentification.setMimeType(readText(reader, 512));
          case ENCODING -> formatIdentification.setEncoding(readText(reader, 512));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(FORMAT_IDENTIFICATION)) {
          break;
        }
      }
    }
  }

  private void readFileInfo(XMLEventReader reader, BinaryDataObject binaryDataObject)
      throws XMLStreamException {
    FileInfo fileInfo = new FileInfo();
    binaryDataObject.setFileInfo(fileInfo);

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case FILENAME -> fileInfo.setFilename(readText(reader, 512));
          case CREATING_APPLICATION_NAME -> fileInfo.setCreatingApplicationName(
              readText(reader, 512));
          case CREATING_APPLICATION_VERSION -> fileInfo.setCreatingApplicationVersion(
              readText(reader, 512));
          case CREATING_OS -> fileInfo.setCreatingOs(readText(reader, 512));
          case CREATING_OS_VERSION -> fileInfo.setCreatingOsVersion(readText(reader, 512));
          case DATE_CREATED_BY_APPLICATION -> fileInfo.setDateCreatedByApplication(
              readDateTime(reader));
          case LAST_MODIFIED -> fileInfo.setLastModified(readDateTime(reader));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(FILE_INFO)) {
          break;
        }
      }
    }
  }

  private void readArchiveUnit(XMLEventReader reader, StartElement element, ArchiveUnit parentUnit)
      throws XMLStreamException, IOException {

    Attribute attr = element.getAttributeByName(XML_ID);
    assertFalse(
        attr == null || StringUtils.isBlank(attr.getValue()),
        "Read archive unit failed",
        "Archive unit must have non null unique id attribute");

    checkMaxArchiveUnits(archiveUnitMap.size());

    ArchiveUnit archiveUnit = new ArchiveUnit();
    archiveUnit.setId(sequence.nextValue());
    archiveUnit.setTenant(tenant);
    archiveUnit.setUnitType(unitType);
    archiveUnit.setSedaVersion(sedaVersion);
    archiveUnit.setImplementationVersion(IMPLEMENTATION_VERSION);
    archiveUnit.setXmlId(attr.getValue());
    archiveUnit.setTransferred(Boolean.FALSE);
    archiveUnit.setCreationDate(LocalDateTime.now());

    // Operations
    archiveUnit.setOperationId(operationId);
    archiveUnit.addToOperationIds(operationId);

    // Parents
    if (parentUnit != null) {
      archiveUnit.setParentId(parentUnit.getId());
      archiveUnit.setParentUnit(parentUnit);
    } else {
      archiveUnit.setDetached();
    }

    // Add to archive unit map
    assertTrue(
        archiveUnitMap.put(archiveUnit.getXmlId(), archiveUnit) == null,
        "Read archive unit failed",
        String.format("ArchiveUnit %s is not unique", archiveUnit.getXmlId()));

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case "ArchiveUnitProfile" -> archiveUnit.setArchiveUnitProfile(
              readText(reader, 512)); // Validation is not yet implemented
          case MANAGEMENT -> archiveUnit = readManagement(reader, archiveUnit);
          case CONTENT -> readContent(reader, archiveUnit);
          case DATA_OBJECT_REFERENCE, DATA_OBJECT_REF -> readDataObjectReference(
              reader, archiveUnit);
          case ARCHIVE_UNIT -> readArchiveUnit(reader, startElement, archiveUnit);
          case ARCHIVE_UNIT_REF_ID -> {
            archiveUnit.setArchiveUnitRefId(readText(reader, 512));
            referrerUnits.add(archiveUnit);
          }
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(ARCHIVE_UNIT)) {
          // Init default inherited rules. EndDate is already computed
          if (operationType != OperationType.INGEST_HOLDING) {
            archiveUnit.initComputedInheritedRules();
          }
          archiveUnit.setUpdateDate(LocalDateTime.now());
          break;
        }
      }
    }
  }

  private void readDataObjectReference(XMLEventReader reader, ArchiveUnit archiveUnit)
      throws XMLStreamException {
    String xmlId = archiveUnit.getXmlId();
    String ref = null;

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        String localPart = startElement.getName().getLocalPart();
        if (localPart.equals(DATA_OBJECT_REFERENCE_ID)
            || localPart.equals(DATA_OBJECT_GROUP_REFERENCE_ID)) {
          assertNull(
              ref,
              READ_DATA_OBJECT_REFERENCE_FAILED,
              String.format("ArchiveUnit '%s' has multiple data object references", xmlId));
          ref = readText(reader, 512);
          assertTrue(
              StringUtils.isNotBlank(ref),
              READ_DATA_OBJECT_REFERENCE_FAILED,
              String.format("ArchiveUnit '%s' has empty data object reference", xmlId));
          DataObjectGroup dog = dataObjectGroupMap.get(ref);
          assertNotNull(
              dog,
              READ_DATA_OBJECT_REFERENCE_FAILED,
              String.format(
                  "ArchiveUnit '%s' references non existent data object '%s'", xmlId, ref));
          setQualifiers(dog, archiveUnit);
        }
      } else if (nextEvent.isEndElement()) {
        String localPart = nextEvent.asEndElement().getName().getLocalPart();
        if (localPart.equals(DATA_OBJECT_REFERENCE) || localPart.equals(DATA_OBJECT_REF)) {
          break;
        }
      }
    }
  }

  private void setQualifiers(DataObjectGroup dog, ArchiveUnit archiveUnit) {
    Map<String, Qualifiers> qMap = new HashMap<>();

    for (var physicalDataObject : dog.getPhysicalDataObjects()) {
      Qualifiers qualifiers = qMap.computeIfAbsent("PhysicalDataObject", k -> new Qualifiers());
      qualifiers.setQualifier(PhysicalQualifier.PhysicalMaster.toString());
      qualifiers.incNbc();
      ObjectVersion version = getObjectVersion(physicalDataObject);
      qualifiers.getVersions().add(version);
    }

    for (var binaryDataObject : dog.getBinaryDataObjects()) {
      String binaryVersion = binaryDataObject.getBinaryVersion();
      String qualifier = UnitUtils.getBinaryQualifier(binaryVersion).toString();
      Qualifiers qualifiers = qMap.computeIfAbsent(qualifier, k -> new Qualifiers());
      qualifiers.setQualifier(qualifier);
      qualifiers.incNbc();
      ObjectVersion version = getObjectVersion(binaryDataObject);
      qualifiers.getVersions().add(version);
    }

    archiveUnit.getQualifiers().addAll(qMap.values());
  }

  private static ObjectVersion getObjectVersion(PhysicalDataObject physicalDataObject) {
    ObjectVersion version = new ObjectVersion();
    version.setXmlId(physicalDataObject.getXmlId());
    version.setId(physicalDataObject.getId());
    version.setPhysicalId(physicalDataObject.getPhysicalId());
    version.setDataObjectVersion(physicalDataObject.getPhysicalVersion());
    version.setMeasure(physicalDataObject.getMeasure());
    return version;
  }

  private static ObjectVersion getObjectVersion(BinaryDataObject binaryDataObject) {
    ObjectVersion version = new ObjectVersion();
    version.setXmlId(binaryDataObject.getXmlId());
    version.setId(binaryDataObject.getId());
    version.setDataObjectVersion(binaryDataObject.getBinaryVersion());
    version.setAlgorithm(binaryDataObject.getDigestAlgorithm());
    version.setMessageDigest(binaryDataObject.getMessageDigest());
    version.setBinaryPath(binaryDataObject.getBinaryPath());
    version.setFileInfo(binaryDataObject.getFileInfo());
    version.setFormatIdentification(binaryDataObject.getFormatIdentification());
    version.setSize(binaryDataObject.getSize());
    version.setOperationId(binaryDataObject.getOperationId());
    return version;
  }

  private void readContent(XMLEventReader reader, ArchiveUnit archiveUnit)
      throws XMLStreamException {

    ArrayList<ObjectNode> nodes = new ArrayList<>();
    nodes.add(JsonNodeFactory.instance.objectNode());

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        String elementName = startElement.getName().getLocalPart();

        switch (elementName) {
          case TITLE:
            String title = readText(reader, 1024);
            assertTrue(
                StringUtils.isNotBlank(title),
                "Read archive unit content failed",
                "Title cannot be empty");
            archiveUnit.setTitle(title);
            break;
          case DESCRIPTION:
            archiveUnit.setDescription(readText(reader, 4096));
            break;
          case DESCRIPTION_LEVEL:
            archiveUnit.setDescriptionLevel(readText(reader, 512));
            break;
          case FILE_PLAN_POSITION:
            archiveUnit.getFilePlanPositions().add(readText(reader, 512));
            break;
          case SYSTEM_ID:
            // We ignore the SystemdId
            break;
          case ORIGINATING_SYSTEM_ID:
            archiveUnit.getOriginatingSystemIds().add(readText(reader, 512));
            break;
          case ARCHIVAL_AGENCY_ARCHIVE_UNIT_IDENTIFIER:
            archiveUnit.getArchivalAgencyArchiveUnitIdentifiers().add(readText(reader, 512));
            break;
          case ORIGINATING_AGENCY_ARCHIVE_UNIT_IDENTIFIER:
            archiveUnit.getOriginatingAgencyArchiveUnitIdentifiers().add(readText(reader, 512));
            break;
          case TRANSFERRING_AGENCY_ARCHIVE_UNIT_IDENTIFIER:
            archiveUnit.getTransferringAgencyArchiveUnitIdentifiers().add(readText(reader, 512));
            break;
          case CUSTODIAL_HISTORY:
            break;
          case CUSTODIAL_HISTORY_ITEM:
            archiveUnit.addCustodialHistoryItem(readText(reader, 512));
            break;
          case ARCHIVE_UNIT_PROFILE:
            archiveUnit.setArchiveUnitProfile(readText(reader, 512));
            break;
          case TYPE:
            archiveUnit.setType(readText(reader, 512));
            break;
          case DOCUMENT_TYPE:
            archiveUnit.setDocumentType(readText(reader, 512));
            break;
          case STATUS:
            archiveUnit.setStatus(readText(reader, 512));
            break;
          case VERSION:
            archiveUnit.setVersion(readText(reader, 512));
            break;
          case TAG:
            archiveUnit.addTag(readText(reader, 512));
            break;
          case ACQUIRED_DATE:
            archiveUnit.setAcquiredDate(readDate(reader));
            break;
          case CREATED_DATE:
            archiveUnit.setCreatedDate(readDate(reader));
            break;
          case END_DATE:
            archiveUnit.setEndDate(readDate(reader));
            break;
          case RECEIVED_DATE:
            archiveUnit.setReceivedDate(readDate(reader));
            break;
          case REGISTERED_DATE:
            archiveUnit.setRegisteredDate(readDate(reader));
            break;
          case START_DATE:
            archiveUnit.setStartDate(readDate(reader));
            break;
          case SENT_DATE:
            archiveUnit.setSentDate(readDate(reader));
            break;
          case TRANSACTED_DATE:
            archiveUnit.setTransactedDate(readDate(reader));
            break;
          case KEYWORD:
            readKeyword(reader, archiveUnit);
            break;
          case FULL_TEXT:
            archiveUnit.setFullText(readText(reader, 131072));
            break;
          default:
            OntologyMap ontologyMap = ontologyMapper.getOntologyMap(archiveUnit.getDocumentType());
            readExtended(reader, startElement, nodes, ontologyMap);
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(CONTENT)) {
          if (nodes.size() == 1) {
            archiveUnit.setExtents(nodes.getFirst());
            return;
          }
          break;
        }
      }
    }
  }

  private void readExtended(
      XMLEventReader reader, StartElement startElt, List<ObjectNode> nodes, OntologyMap ontologyMap)
      throws XMLStreamException {

    ArrayList<String> names = new ArrayList<>();
    names.add(startElt.getName().getLocalPart());
    nodes.add(JsonNodeFactory.instance.objectNode());
    String buffer = null;
    boolean isElementStarted = true;

    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        StartElement startElement = event.asStartElement();
        names.add(startElement.getName().getLocalPart());
        nodes.add(JsonNodeFactory.instance.objectNode());
        isElementStarted = true;
        buffer = null;

      } else if (event.isCharacters()) {
        buffer = event.asCharacters().getData();

      } else if (event.isEndElement()) {
        ObjectNode beforeNode = nodes.get(nodes.size() - 2);
        ObjectNode lastNode = nodes.getLast();
        String lastName = names.getLast();

        if (isElementStarted) {
          if (StringUtils.isBlank(buffer)) {
            buffer = "";
          }
          String srcName = String.join(".", names);
          checkOntologyKey(ontologyMap, srcName, buffer);

          JsonNode value = beforeNode.get(lastName);
          if (value == null) {
            beforeNode.put(lastName, buffer);
          } else if (value.isArray()) {
            ((ArrayNode) value).add(buffer);
          } else {
            beforeNode.putArray(lastName).add(value).add(buffer);
          }
        } else {
          JsonNode value = beforeNode.get(lastName);
          if (value == null) {
            beforeNode.set(lastName, lastNode);
          } else if (value.isArray()) {
            ((ArrayNode) value).add(lastNode);
          } else {
            beforeNode.putArray(lastName).add(value).add(lastNode);
          }
        }

        nodes.removeLast();
        if (nodes.size() == 1) {
          return;
        }
        names.removeLast();
        isElementStarted = false;
        buffer = null;
      }
    }
  }

  private void readKeyword(XMLEventReader reader, ArchiveUnit archiveUnit)
      throws XMLStreamException {
    String reference = null;
    String content = null;

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case KEYWORD_REFERENCE -> reference = readText(reader, 512);
          case KEYWORD_CONTENT -> content = readText(reader, 1024);
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(KEYWORD)) {
          if (StringUtils.isNotBlank(reference) && StringUtils.isNotBlank(content)) {
            // Do not verify ontology for keyword.reference/content tags
            archiveUnit.addKeyValue(reference, content);
          }
          break;
        }
      }
    }
  }

  private ArchiveUnit readManagement(XMLEventReader reader, ArchiveUnit archiveUnit)
      throws XMLStreamException, IOException {
    Management management = new Management();

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case APPRAISAL_RULE -> management.setAppraisalRules(readAppraisalRules(reader));
          case ACCESS_RULE -> management.setAccessRules(readAccessRules(reader));
          case CLASSIFICATION_RULE -> management.setClassificationRules(
              readClassificationRules(reader));
          case DISSEMINATION_RULE -> management.setDisseminationRules(
              readDisseminationRules(reader));
          case STORAGE_RULE -> management.setStorageRules(readStorageRules(reader));
          case REUSE_RULE -> management.setReuseRules(readReuseRules(reader));
          case HOLD_RULE -> management.setHoldRules(readHoldRules(reader));
          case UPDATE_OPERATION -> archiveUnit = readUpdateOperation(reader, archiveUnit);
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(MANAGEMENT)) {
          if (management.hasRules()) {
            assertTrue(
                operationType != OperationType.INGEST_HOLDING,
                "Read management failed",
                "Management Rules are not allowed in holding plans");
            checkManagementRules(management);
          }
          archiveUnit.setManagement(management);
          break;
        }
      }
    }
    return archiveUnit;
  }

  private AppraisalRules readAppraisalRules(XMLEventReader reader) throws XMLStreamException {

    AppraisalRules rules = new AppraisalRules();
    Rule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new Rule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, "Read appraisal rule failed", RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
          case DURATION -> rules.setDuration(readText(reader, 512));
          case FINAL_ACTION -> rules.setFinalAction(readText(reader, 512));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(APPRAISAL_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private ClassificationRules readClassificationRules(XMLEventReader reader)
      throws XMLStreamException {

    ClassificationRules rules = new ClassificationRules();
    Rule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new Rule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, "Read classification rule failed", RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
          case CLASSIFICATION_LEVEL -> rules.setClassificationLevel(readText(reader, 512));
          case CLASSIFICATION_AUDIENCE -> rules.setClassificationAudience(readText(reader, 512));
          case CLASSIFICATION_OWNER -> rules.setClassificationOwner(readText(reader, 512));
          case CLASSIFICATION_REASSESSING_DATE -> rules.setClassificationReassessingDate(
              readDate(reader));
          case NEED_REASSESSING_AUTHORIZATION -> rules.setNeedReassessingAuthorization(
              readBoolean(reader));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(CLASSIFICATION_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private HoldRules readHoldRules(XMLEventReader reader) throws XMLStreamException {

    HoldRules rules = new HoldRules();
    HoldRule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new HoldRule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, READ_HOLD_RULE_FAILED, RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case HOLD_OWNER -> {
            assertNotNull(
                rule, READ_HOLD_RULE_FAILED, "Rule name must be declared before hold owner");
            rule.setHoldOwner(readText(reader, 512));
          }
          case HOLD_REASON -> {
            assertNotNull(
                rule, READ_HOLD_RULE_FAILED, "Rule name must be declared before hold reason");
            rule.setHoldReason(readText(reader, 1024));
          }
          case HOLD_END_DATE -> {
            assertNotNull(
                rule, READ_HOLD_RULE_FAILED, "Rule name must be declared before hold end date");
            rule.setHoldEndDate(readDate(reader));
          }
          case REASSESSING_DATE -> {
            assertNotNull(
                rule,
                READ_HOLD_RULE_FAILED,
                "Rule name must be declared before hold reassessing date");
            rule.setHoldReassessingDate(readDate(reader));
          }
          case PREVENT_REARRANGEMENT -> {
            assertNotNull(
                rule,
                READ_HOLD_RULE_FAILED,
                "Rule name must be declared before hold prevent rearrangement");
            rule.setPreventRearrangement(readBoolean(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(HOLD_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private StorageRules readStorageRules(XMLEventReader reader) throws XMLStreamException {

    StorageRules rules = new StorageRules();
    Rule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new Rule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, "Read storage rule failed", RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
          case FINAL_ACTION -> rules.setFinalAction(readText(reader, 512));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(STORAGE_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private AccessRules readAccessRules(XMLEventReader reader) throws XMLStreamException {

    AccessRules rules = new AccessRules();
    Rule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new Rule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, "Read access rule failed", RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(ACCESS_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private DisseminationRules readDisseminationRules(XMLEventReader reader)
      throws XMLStreamException {

    DisseminationRules rules = new DisseminationRules();
    Rule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new Rule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, "Read dissemination rule failed", RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(DISSEMINATION_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private ReuseRules readReuseRules(XMLEventReader reader) throws XMLStreamException {

    ReuseRules rules = new ReuseRules();
    Rule rule = null;
    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case RULE -> {
            rule = new Rule(readText(reader, 512));
            rules.addRule(rule);
          }
          case START_DATE -> {
            assertNotNull(rule, "Read reuse rule failed", RULE_NAME_AFTER_START_DATE);
            rule.setStartDate(readDate(reader));
          }
          case PREVENT_INHERITANCE -> rules
              .getRuleInheritance()
              .setPreventInheritance(readBoolean(reader));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(REUSE_RULE)) {
          break;
        }
      }
    }
    return rules;
  }

  private ArchiveUnit readUpdateOperation(XMLEventReader reader, ArchiveUnit archiveUnit)
      throws XMLStreamException, IOException {

    assertTrue(
        archiveUnit.isDetached(),
        "Read update operation failed",
        "Update Operation is not supported on nested archive unit");

    UpdateOperation updateOperation = new UpdateOperation();

    while (reader.hasNext()) {
      XMLEvent nextEvent = reader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
          case SYSTEM_ID -> updateOperation.setSystemId(readText(reader, 512));
          case METADATA_NAME -> updateOperation.setMetadataName(readText(reader, 512));
          case METADATA_VALUE -> updateOperation.setMetadataValue(readText(reader, 512));
        }
      } else if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals(UPDATE_OPERATION)) {

          String systemId = updateOperation.getSystemId();
          if (StringUtils.isNotBlank(systemId)) {
            if (!StringUtils.isNumeric(systemId)) {
              throw new ManifestException(
                  "Read update operation failed",
                  String.format("Bad SystemId '%s' in update operation", systemId));
            }
            ArchiveUnit linkUnit = getLinkUnit(Long.valueOf(systemId));
            archiveUnit = getUpdatedUnit(archiveUnit, linkUnit);

          } else if (StringUtils.isNotBlank(updateOperation.getMetadataName())
              && StringUtils.isNotBlank(updateOperation.getMetadataValue())) {

            ArchiveUnit linkUnit =
                getLinkUnit(updateOperation.getMetadataName(), updateOperation.getMetadataValue());
            archiveUnit = getUpdatedUnit(archiveUnit, linkUnit);
          }
          break;
        }
      }
    }
    return archiveUnit;
  }

  private ArchiveUnit getUpdatedUnit(ArchiveUnit archiveUnit, ArchiveUnit linkUnit) {
    checkAttachedUnitType(unitType, linkUnit.getUnitType());
    // This archive unit already exists and must not be created
    archiveUnitMap.remove(archiveUnit.getXmlId());
    rootUnits.put(linkUnit.getId(), linkUnit);
    return linkUnit;
  }

  // This call will leave the reader at the matching END_ELEMENT
  private static String readText(XMLEventReader reader, int maxSize) throws XMLStreamException {
    String text = reader.getElementText();
    assertFalse(text.length() > maxSize, "Read archive unit content failed", "Text is too long");
    return text;
  }

  private static Double readDouble(XMLEventReader reader) throws XMLStreamException {
    return Double.valueOf(reader.getElementText());
  }

  private static Boolean readBoolean(XMLEventReader reader) throws XMLStreamException {
    return Boolean.valueOf(reader.getElementText());
  }

  private static LocalDate readDate(XMLEventReader reader) throws XMLStreamException {
    return DateUtils.parseToLocalDate(reader.getElementText());
  }

  private static LocalDateTime readDateTime(XMLEventReader reader) throws XMLStreamException {
    return DateUtils.parseToLocalDateTime(reader.getElementText());
  }
}
