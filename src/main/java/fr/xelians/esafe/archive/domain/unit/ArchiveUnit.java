/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMap;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitIndex;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.domain.unit.rules.computed.ComputedInheritedRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.Management;
import fr.xelians.esafe.common.entity.searchengine.DocumentSe;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.search.domain.field.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ArchiveUnit implements DocumentSe {

  public static final Long ROOT = -1L;
  public static final Long DETACHED = -2L;
  public static final Long IGNORE = -3L;

  @JsonIgnore
  @JsonProperty("XmlId")
  protected String xmlId;

  @ToString.Exclude @JsonIgnore protected ArchiveUnit parentUnit;

  @ToString.Exclude @JsonIgnore
  protected HashMap<String, ArchiveUnit> childUnitMap = new HashMap<>();

  @JsonIgnore protected String archiveUnitRefId;

  @JsonProperty("_creationDate")
  protected LocalDateTime creationDate;

  @JsonProperty("_updateDate")
  protected LocalDateTime updateDate;

  @JsonProperty("_unitId")
  protected Long id;

  @JsonProperty("_objectId")
  protected Long objectId;

  @JsonProperty("_tenant")
  protected Long tenant;

  // Parent unit id
  @JsonProperty("_up")
  protected Long parentId;

  // All parent units id up to the root unit
  @JsonProperty("_us")
  protected List<Long> parentIds = new ArrayList<>();

  // Service producer
  @JsonProperty("_sp")
  protected String serviceProducer;

  // All service producers
  @JsonProperty("_sps")
  protected HashSet<String> serviceProducers = new HashSet<>();

  @JsonProperty("_opi")
  protected Long operationId;

  @JsonProperty("_ops")
  protected List<Long> operationIds = new ArrayList<>();

  @JsonProperty("_unitType")
  protected UnitType unitType;

  @JsonProperty("_sedaVersion")
  protected String sedaVersion;

  @JsonProperty("_implementationVersion")
  protected String implementationVersion;

  @JsonProperty("_lifeCycles")
  protected List<LifeCycle> lifeCycles = new ArrayList<>();

  //    @JsonProperty("_ac")
  //    protected long attachmentCounter = 0;
  //
  //    @JsonProperty("_rc")
  //    protected long reclassificationCounter = 0;

  @JsonProperty("_av")
  protected int autoversion = 1;

  @JsonProperty("_min")
  protected int min;

  @JsonProperty("_max")
  protected int max;

  // This field in stored but not indexed
  @JsonProperty("_extents")
  protected JsonNode extents;

  // Extended key values
  @JsonProperty("_ext")
  protected JsonNode ext;

  // Parents Ids used for Depth
  @JsonProperty("_ups")
  protected JsonNode ups;

  @JsonProperty("_mgt")
  protected Management management;

  @JsonProperty("_cir")
  protected ComputedInheritedRules computedInheritedRules;

  @JsonProperty("_validCir")
  protected final boolean validComputedInheritedRules = true;

  @JsonProperty("_qualifiers")
  protected List<Qualifiers> qualifiers = new ArrayList<>();

  @JsonProperty("_nbObjects")
  protected int nbObjects = 0;

  //  protected List<Event> logEvents = new ArrayList<>();

  @JsonProperty("Title")
  protected String title;

  @JsonProperty("Description")
  protected String description;

  @JsonProperty("CustodialHistoryItems")
  protected List<String> custodialHistoryItems = new ArrayList<>();

  @JsonProperty("Tags")
  protected List<String> tags = new ArrayList<>();

  @JsonProperty("Keyword")
  protected List<KeyTag> keyTags = new ArrayList<>();

  @JsonProperty("_keywords")
  protected List<String> keywords = new ArrayList<>();

  @JsonProperty("ArchiveUnitProfile")
  protected String archiveUnitProfile;

  @JsonProperty("DescriptionLevel")
  protected String descriptionLevel;

  @JsonProperty("FilePlanPosition")
  protected List<String> filePlanPositions = new ArrayList<>();

  @JsonProperty("OriginatingSystemId")
  protected List<String> originatingSystemIds = new ArrayList<>();

  @JsonProperty("ArchivalAgencyArchiveUnitIdentifier")
  protected List<String> archivalAgencyArchiveUnitIdentifiers = new ArrayList<>();

  @JsonProperty("OriginatingAgencyArchiveUnitIdentifier")
  protected List<String> originatingAgencyArchiveUnitIdentifiers = new ArrayList<>();

  @JsonProperty("TransferringAgencyArchiveUnitIdentifier")
  protected List<String> transferringAgencyArchiveUnitIdentifiers = new ArrayList<>();

  @JsonProperty("Type")
  protected String type;

  @JsonProperty("DocumentType")
  protected String documentType;

  //  @JsonProperty("Languages")
  //  protected List<String> languages = new ArrayList<>();

  //  @JsonProperty("DescriptionLanguage")
  //  protected String descriptionLanguage;

  @JsonProperty("Status")
  protected String status;

  @JsonProperty("Version")
  protected String version;

  @JsonProperty("CreatedDate")
  protected LocalDate createdDate;

  @JsonProperty("TransactedDate")
  protected LocalDate transactedDate;

  @JsonProperty("AcquiredDate")
  protected LocalDate acquiredDate;

  @JsonProperty("SentDate")
  protected LocalDate sentDate;

  @JsonProperty("ReceivedDate")
  protected LocalDate receivedDate;

  @JsonProperty("RegisteredDate")
  protected LocalDate registeredDate;

  @JsonProperty("StartDate")
  protected LocalDate startDate;

  @JsonProperty("EndDate")
  protected LocalDate endDate;

  @JsonProperty("FullText")
  protected String fullText;

  public ArchiveUnit() {}

  //  @JsonProperty("SystemId")
  //  public void setSystemId(String str) {
  //    // Do nothing - SystemId is an alias for id
  //  }

  //  @JsonProperty("SystemId")
  //  public String getSystemId() {
  //    return id.toString();
  //  }

  @JsonIgnore
  public boolean isDetached() {
    return Objects.equals(parentId, ArchiveUnit.DETACHED);
  }

  @JsonIgnore
  public void setDetached() {
    parentId = ArchiveUnit.DETACHED;
  }

  @JsonIgnore
  public boolean isIgnored() {
    return Objects.equals(parentId, ArchiveUnit.IGNORE);
  }

  @JsonIgnore
  public void setIgnored() {
    parentId = ArchiveUnit.IGNORE;
  }

  @JsonIgnore
  public boolean isRoot() {
    return Objects.equals(id, ROOT);
  }

  public void setParentUnit(ArchiveUnit newParentUnit) {
    if (parentUnit != null) {
      parentUnit.getChildUnitMap().remove(this.getXmlId());
    }
    if (newParentUnit != null) {
      newParentUnit.getChildUnitMap().put(this.getXmlId(), this);
    }
    this.parentUnit = newParentUnit;
  }

  public void removeParentUnit() {
    if (this.parentUnit != null) {
      this.parentUnit.getChildUnitMap().remove(this.getXmlId());
    }
    this.parentUnit = null;
  }

  public void addToParentIds(Long parentId) {
    parentIds.add(parentId);
  }

  public void addToParentIds(List<Long> parIds) {
    parentIds.addAll(parIds);
  }

  public void addToServiceProviders(String sp) {
    serviceProducers.add(sp);
  }

  public void addToServiceProviders(Set<String> sps) {
    serviceProducers.addAll(sps);
  }

  public void addToOperationIds(Long operationId) {
    operationIds.add(operationId);
  }

  //  public void addLogEvent(Event event) {
  //    Validate.notNull(event, Utils.NOT_NULL, "event");
  //    logEvents.add(event);
  //  }

  //  public void addLanguage(String language) {
  //    Validate.notNull(language, Utils.NOT_NULL, "language");
  //    languages.add(language);
  //  }

  public void addCustodialHistoryItem(String item) {
    Validate.notNull(item, Utils.NOT_NULL, "item");
    custodialHistoryItems.add(item);
  }

  public void addFilePlanPosition(String item) {
    Validate.notNull(item, Utils.NOT_NULL, "item");
    filePlanPositions.add(item);
  }

  public void addOriginatingSystemId(String item) {
    Validate.notNull(item, Utils.NOT_NULL, "item");
    originatingSystemIds.add(item);
  }

  public void addArchivalAgencyArchiveUnitIdentifier(String item) {
    Validate.notNull(item, Utils.NOT_NULL, "item");
    archivalAgencyArchiveUnitIdentifiers.add(item);
  }

  public void addOriginatingAgencyArchiveUnitIdentifier(String item) {
    Validate.notNull(item, Utils.NOT_NULL, "item");
    originatingAgencyArchiveUnitIdentifiers.add(item);
  }

  public void addTransferringAgencyArchiveUnitIdentifier(String item) {
    Validate.notNull(item, Utils.NOT_NULL, "item");
    transferringAgencyArchiveUnitIdentifiers.add(item);
  }

  //    public void incAttachmentCounter() {
  //        attachmentCounter++;
  //    }
  //
  //    public void incReclassificationCounter() {
  //        reclassificationCounter++;
  //    }

  public void addLifeCycle(LifeCycle lfc) {
    Validate.notNull(lfc, Utils.NOT_NULL, "lfc");
    lifeCycles.add(lfc);
    autoversion++;
  }

  public void removeLifeCycle() {
    if (!lifeCycles.isEmpty()) {
      lifeCycles.removeLast();
      autoversion--;
    }
  }

  public void addTag(String tag) {
    Validate.notNull(tag, Utils.NOT_NULL, "tag");
    tags.add(tag);
  }

  public void addKeyValue(String key, String value) {
    Validate.notNull(key, Utils.NOT_NULL, "key");
    Validate.notNull(value, Utils.NOT_NULL, "value");
    keyTags.add(new KeyTag(key, value));
  }

  @JsonIgnore
  public boolean isParentRoot() {
    return Objects.equals(parentId, ROOT);
  }

  public void initComputedInheritedRules() {
    computedInheritedRules = new ComputedInheritedRules();

    if (management != null) {
      if (management.getAccessRules() != null) {
        computedInheritedRules.setAccessRules(management.getAccessRules());
      }
      if (management.getAppraisalRules() != null) {
        computedInheritedRules.setAppraisalRules(management.getAppraisalRules());
      }
      if (management.getClassificationRules() != null) {
        computedInheritedRules.setClassificationRules(management.getClassificationRules());
      }
      if (management.getDisseminationRules() != null) {
        computedInheritedRules.setDisseminationRules(management.getDisseminationRules());
      }
      if (management.getReuseRules() != null) {
        computedInheritedRules.setReuseRules(management.getReuseRules());
      }
      if (management.getStorageRules() != null) {
        computedInheritedRules.setStorageRules(management.getStorageRules());
      }
      if (management.getHoldRules() != null) {
        computedInheritedRules.setHoldRules(management.getHoldRules());
      }
    }
  }

  /*
   * Build the json search engine properties and ensure that extents are compatible
   * with the fields defined in the archive unit index mapping.
   * This method must always be called in the check phase of the relevant tasks
   * to avoid a nasty indexing exception in the indexing phase.
   *
   * @param ontology mapper
   */
  @JsonIgnore
  public void buildProperties(OntologyMapper ontologyMapper) {
    updateDataObjects();
    updateKeywords();
    min = max = parentIds.size();
    ext = createExtNode(ontologyMapper);
    ups = createUpsNode();
  }

  private void updateKeywords() {
    keywords = keyTags.stream().map(kt -> kt.key() + ":" + kt.value()).toList();
  }

  private void updateDataObjects() {
    if (qualifiers.isEmpty()) {
      objectId = null;
      nbObjects = 0;
    } else {
      objectId = id;
      nbObjects =
          qualifiers.stream()
              .filter(Qualifiers::isBinaryQualifier)
              .mapToInt(Qualifiers::getNbc)
              .sum();
    }
  }

  private JsonNode createUpsNode() {
    ObjectNode upsNode = JsonNodeFactory.instance.objectNode();
    for (int i = 2, parentSize = parentIds.size(); i <= ArchiveUnitIndex.UPS_SIZE; i++) {
      ArrayNode arrayNode = upsNode.putArray("_up" + i);
      parentIds.subList(0, Math.min(i, parentSize)).forEach(arrayNode::add);
    }
    return upsNode;
  }

  private JsonNode createExtNode(OntologyMapper ontologyMapper) {
    ObjectNode extNode = JsonNodeFactory.instance.objectNode();
    OntologyMap ontologyMap = ontologyMapper.getOntologyMap(documentType);

    for (KeyTag kt : JsonUtils.visit(extents)) {
      String key = getMappedKey(ontologyMap, kt.key(), kt.value());
      ArrayNode arrayNode = (ArrayNode) extNode.get(key);
      if (arrayNode == null) {
        arrayNode = extNode.putArray(key);
      }
      arrayNode.add(kt.value());
    }
    return extNode;
  }

  private static String getMappedKey(OntologyMap ontologyMap, String srcKey, String value) {
    if (ontologyMap != null) {
      String dstKey = ontologyMap.get(srcKey);
      if (StringUtils.isNotBlank(dstKey)) {
        return checkKey(dstKey, value);
      }
    }
    return checkKey(srcKey, value);
  }

  private static String checkKey(String key, String value) {
    Field field = ArchiveUnitIndex.INSTANCE.getField(key);
    if (field == null) {
      throw new InternalException(
          "Failed to check field in archive unit",
          String.format("Unknown field with key '%s' and value '%s'", key, value));
    }
    if (!field.isValid(value)) {
      throw new InternalException(
          "Failed to check field in archive unit",
          String.format("Invalid field value with key '%s' and value '%s'", key, value));
    }
    return key;
  }
}
