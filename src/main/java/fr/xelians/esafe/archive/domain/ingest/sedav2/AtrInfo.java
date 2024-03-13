/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import fr.xelians.esafe.archive.domain.unit.ArchiveTransfer;

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
