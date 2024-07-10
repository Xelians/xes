package fr.xelians.esafe.accession.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.entity.searchengine.DocumentSe;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RegisterSummary implements DocumentSe {

  @JsonProperty("_summaryId")
  private Long id;

  @JsonProperty("_tenant")
  private Long tenant;

  @JsonProperty("_v")
  private long version = 0;

  @JsonProperty("OriginatingAgency")
  private String originatingAgency;

  @JsonProperty("CreationDate")
  private LocalDateTime creationDate;

  @JsonProperty("TotalObjectGroups")
  private ValueDetail totalObjectsGroups = new ValueDetail(0);

  @JsonProperty("TotalUnits")
  private ValueDetail totalUnits = new ValueDetail(0);

  @JsonProperty("TotalObjects")
  private ValueDetail totalObjects = new ValueDetail(0);

  @JsonProperty("ObjectSize")
  private ValueDetail objectSize = new ValueDetail(0);

  /** List of operation id */
  @JsonProperty("OperationIds")
  private List<Long> operationIds = new ArrayList<>();

  public void incVersion() {
    version++;
  }

  public void addOperationId(Long operationsId) {
    operationIds.add(operationsId);
  }
}
