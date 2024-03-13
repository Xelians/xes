/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.atr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DataObjectGroupReply {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  private String xmlId;

  @JsonProperty("PhysicalDataObjects")
  private List<PhysicalDataObjectReply> physicalDataObjectReplys = new ArrayList<>();

  @JsonProperty("BinaryDataObjects")
  private List<BinaryDataObjectReply> binaryDataObjectReplys = new ArrayList<>();
}
