/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArchiveTransfer extends Message {

  @JsonProperty("ArchivalAgreement")
  protected String archivalAgreement;

  @JsonProperty("ArchivalAgency")
  protected Agency archivalAgency;

  @JsonProperty("TransferringAgency")
  protected Agency transferringAgency;

  @JsonProperty("Created")
  protected LocalDateTime created = LocalDateTime.now();
}
