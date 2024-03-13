/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CustodialHistoryModelDto {

  @JsonProperty("CustodialHistoryItem")
  private List<String> custodialHistoryItem = new ArrayList<>();

  private DataObjectReference custodialHistoryFile;

  public CustodialHistoryModelDto() {}
}
