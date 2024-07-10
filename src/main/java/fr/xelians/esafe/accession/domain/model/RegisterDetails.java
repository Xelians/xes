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
public class RegisterDetails implements DocumentSe {

  // Equals the first ingest operation id
  @JsonProperty("_detailsId")
  private Long id;

  @JsonProperty("_tenant")
  private Long tenant;

  @JsonProperty("_v")
  private long version = 0;

  @JsonProperty("OriginatingAgency")
  private String originatingAgency;

  @JsonProperty("ArchivalProfile")
  private String archivalProfile;

  @JsonProperty("SubmissionAgency")
  private String submissionAgency;

  @JsonProperty("ArchivalAgreement")
  private String archivalAgreement;

  @JsonProperty("AcquisitionInformation")
  private String acquisitionInformation;

  @JsonProperty("LegalStatus")
  private String legalStatus;

  @JsonProperty("EndDate")
  private LocalDateTime endDate;

  @JsonProperty("StartDate")
  private LocalDateTime startDate;

  @JsonProperty("LastUpdate")
  private LocalDateTime lastUpdate;

  @JsonProperty("Status")
  private RegisterStatus status;

  @JsonProperty("TotalObjectGroups")
  private ValueDetail totalObjectsGroups = new ValueDetail(0);

  @JsonProperty("TotalUnits")
  private ValueDetail totalUnits = new ValueDetail(0);

  @JsonProperty("TotalObjects")
  private ValueDetail totalObjects = new ValueDetail(0);

  @JsonProperty("ObjectSize")
  private ValueDetail objectSize = new ValueDetail(0);

  // Last operation id ?
  @JsonProperty("Opc")
  private Long opc;

  // First operation id ?
  @JsonProperty("Opi")
  private Long opi;

  // First operation type ?
  @JsonProperty("OpType")
  private String operationType;

  /** Operation ingest (origin of creation of the current detail */
  @JsonProperty("Events")
  private List<ValueEvent> events = new ArrayList<>();

  /** List of operation id */
  @JsonProperty("OperationIds")
  private List<Long> operationIds = new ArrayList<>();

  @JsonProperty("obIdIn")
  private String obIdIn;

  @JsonProperty("Comments")
  private List<String> comments = new ArrayList<>();

  public void incVersion() {
    version++;
  }

  public void addEvent(ValueEvent event) {
    events.add(event);
  }

  public void addComment(String comment) {
    comments.add(comment);
  }

  public void addOperationId(Long operationsId) {
    operationIds.add(operationsId);
  }
}
