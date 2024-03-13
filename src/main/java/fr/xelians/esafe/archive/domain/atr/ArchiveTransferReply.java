/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.atr;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArchiveTransferReply extends Message {

  @JsonProperty("ArchivalAgreement")
  protected String archivalAgreement;

  @JsonProperty("ArchivalAgencyIdentifier")
  protected String archivalAgencyIdentifier;

  @JsonProperty("TransferringAgencyIdentifier")
  protected String transferringAgencyIdentifier;

  @JsonProperty("GrantDate")
  protected LocalDateTime grantDate = LocalDateTime.now();

  @JsonProperty("DataObjectGroups")
  protected List<DataObjectGroupReply> dataObjectGroupReplys;

  @JsonProperty("ArchiveUnits")
  protected List<ArchiveUnitReply> archiveUnitReplys;

  @JsonProperty("ReplyCode")
  protected String replyCode;

  @JsonProperty("MessageRequestIdentifier")
  protected String messageRequestIdentifier;

  @JsonIgnore
  public Map<String, ArchiveUnitReply> getArchiveUnitReplyMap() {
    return archiveUnitReplys.stream()
        .collect(toMap(ArchiveUnitReply::getXmlId, Function.identity()));
  }

  @JsonIgnore
  public Map<String, PhysicalDataObjectReply> getPhysicalDataObjectMap() {
    return dataObjectGroupReplys.stream()
        .flatMap(d -> d.getPhysicalDataObjectReplys().stream())
        .collect(toMap(PhysicalDataObjectReply::getXmlId, Function.identity()));
  }

  @JsonIgnore
  public Map<String, BinaryDataObjectReply> getBinaryDataObjectReplyMap() {
    return dataObjectGroupReplys.stream()
        .flatMap(d -> d.getBinaryDataObjectReplys().stream())
        .collect(toMap(BinaryDataObjectReply::getXmlId, Function.identity()));
  }
}
