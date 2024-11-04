/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import fr.xelians.esafe.archive.domain.unit.ArchiveTransfer;

/*
 * @author Emmanuel Deviller
 */
public record AtrInfo(
    String messageIdentifier,
    String archivalAgreement,
    String messageRequestIdentifier,
    String archivalAgencyIdentifier,
    String transferringAgencyIdentifier) {

  public AtrInfo {
    if (messageIdentifier == null) messageIdentifier = "";
    if (archivalAgreement == null) archivalAgreement = "";
    if (messageRequestIdentifier == null) messageRequestIdentifier = "";
    if (archivalAgencyIdentifier == null) archivalAgencyIdentifier = "";
    if (transferringAgencyIdentifier == null) transferringAgencyIdentifier = "";
  }

  public AtrInfo() {
    this("", "", "", "", "");
  }

  public AtrInfo(ArchiveTransfer atr) {
    // Intellij is weird: atr.getArchivalAgency() and atr.getTransferringAgency() may be null
    this(
        atr.getMessageIdentifier(),
        atr.getArchivalAgreement(),
        atr.getMessageIdentifier(),
        atr.getArchivalAgency() == null ? "" : atr.getArchivalAgency().identifier(),
        atr.getTransferringAgency() == null ? "" : atr.getTransferringAgency().identifier());
  }
}
