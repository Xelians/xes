/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import fr.xelians.esafe.common.exception.functional.ManifestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public final class Sedav2Utils {

  public static final String UNKNOWN = "unknown";
  public static final String SEDA_V21 = "fr:gouv:culture:archivesdefrance:seda:v2.1";
  public static final String SEDA_21 = "fr:gouv:culture:archivesdefrance:seda:2.1";
  public static final String SEDA_V22 = "fr:gouv:culture:archivesdefrance:seda:v2.2";
  public static final String SEDA_22 = "fr:gouv:culture:archivesdefrance:seda:2.2";
  public static final String SEDA_V2 = "fr:gouv:culture:archivesdefrance:seda:v2";
  public static final String SEDA_2 = "fr:gouv:culture:archivesdefrance:seda:2";

  private static final XMLInputFactory INPUT_FACTORY;

  static {
    INPUT_FACTORY = XMLInputFactory.newInstance();
    INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
  }

  private Sedav2Utils() {}

  public static XMLInputFactory getInputFactory() {
    return INPUT_FACTORY;
  }

  public static String getArchiveTransferNameSpace(Path manifestPath)
      throws XMLStreamException, IOException {
    try (InputStream is = Files.newInputStream(manifestPath)) {
      XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(is);
      while (reader.hasNext()) {
        XMLEvent nextEvent = reader.nextEvent();
        if (nextEvent.isStartElement()) {
          QName qname = nextEvent.asStartElement().getName();
          if ("ArchiveTransfer".equals(qname.getLocalPart())) {
            return qname.getNamespaceURI().toLowerCase();
          }
        }
      }
    }
    return UNKNOWN;
  }

  public static Sedav2Validator getSedav2Validator(Path manifestPath)
      throws XMLStreamException, IOException {
    return switch (getArchiveTransferNameSpace(manifestPath)) {
      case SEDA_V21, SEDA_21 -> Sedav2Validator.getV21Instance();
      case SEDA_V22, SEDA_22, SEDA_V2, SEDA_2 -> Sedav2Validator.getV22Instance();
      default -> throw new ManifestException(
          "Failed to determine SEDA version", "Version is unknown");
    };
  }
}
