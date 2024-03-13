/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest;

import static fr.xelians.esafe.archive.domain.unit.ArchiveUnit.ROOT;

import fr.xelians.esafe.archive.domain.ingest.sedav2.Sedav2Validator;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitIndex;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.object.*;
import fr.xelians.esafe.archive.domain.unit.rules.management.Management;
import fr.xelians.esafe.archive.service.DateRuleService;
import fr.xelians.esafe.archive.service.IngestService;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.common.exception.functional.ManifestException;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.CheckParentLinkStatus;
import fr.xelians.esafe.referential.domain.ProfileFormat;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AgencyDb;
import fr.xelians.esafe.referential.entity.IngestContractDb;
import fr.xelians.esafe.referential.entity.ProfileDb;
import fr.xelians.esafe.referential.entity.RuleDb;
import fr.xelians.esafe.referential.service.AgencyService;
import fr.xelians.esafe.referential.service.IngestContractService;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.referential.service.ProfileService;
import fr.xelians.esafe.search.domain.field.Field;
import fr.xelians.esafe.sequence.SequenceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.xml.validation.Validator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xml.sax.SAXException;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;

@Slf4j
public abstract class AbstractManifestParser {

  protected static final int UNIT_MAX = 99999999;
  public static final String ARCHIVAL_PROFILE_FAILED = "Check archival profile failed";
  public static final String BINARY_FORMAT_FAILED = "Check binary format failed";
  public static final String PHYSICAL_VERSION_FAILED = "Check physical version failed";
  public static final String BINARY_VERSION_FAILED = "Check binary version failed";
  public static final String PARENTS_FAILED = "Check link parents failed";

  protected final AgencyService agencyService;
  protected final OntologyService ontologyService;
  protected final IngestContractService ingestContractService;
  protected final ProfileService profileService;
  protected final TenantService tenantService;
  protected final SearchService searchService;
  protected final SequenceService sequenceService;
  protected final DateRuleService dateRuleService;

  protected final Long tenant;
  protected final Long operationId;
  protected final OperationType operationType;
  protected final ArchiveUnit rootUnit;

  @Getter protected final OntologyMapper ontologyMapper;

  private final Map<Long, ArchiveUnit> linkSystemIdMap = new HashMap<>();
  private final Map<Key<String, String>, ArchiveUnit> linkKeyMap = new HashMap<>();
  private final Map<String, RuleDb> ruleMap = new HashMap<>();
  private final Map<String, AgencyDb> agencyMap = new HashMap<>();

  private IngestContractDb ingestContractDb;

  // test only
  protected AbstractManifestParser() {
    this.tenantService = null;
    this.searchService = null;
    this.dateRuleService = null;
    this.agencyService = null;
    this.ontologyService = null;
    this.ingestContractService = null;
    this.profileService = null;
    this.sequenceService = null;

    this.tenant = null;
    this.operationId = null;
    this.operationType = null;
    this.ontologyMapper = null;

    this.rootUnit = null;
  }

  protected AbstractManifestParser(IngestService ingestService, OperationDb operation) {
    this.tenantService = ingestService.getTenantService();
    this.searchService = ingestService.getSearchService();
    this.dateRuleService = ingestService.getDateRuleService();
    this.agencyService = ingestService.getReferentialService().getAgencyService();
    this.ontologyService = ingestService.getReferentialService().getOntologyService();
    this.ingestContractService = ingestService.getReferentialService().getIngestContractService();
    this.profileService = ingestService.getReferentialService().getProfileService();
    this.sequenceService = ingestService.getReferentialService().getSequenceService();

    this.tenant = operation.getTenant();
    this.operationId = operation.getId();
    this.operationType = operation.getType();
    this.ontologyMapper = ontologyService.createMapper(tenant);

    this.rootUnit = createRootUnit(tenant);
  }

  public IngestContractDb getIngestContract() {
    return ingestContractDb;
  }

  private static ArchiveUnit createRootUnit(Long tenant) {
    ArchiveUnit unit = new ArchiveUnit();
    unit.setTenant(tenant);
    unit.setId(ROOT);
    unit.setParentId(ROOT);
    unit.setParentUnit(null);
    unit.setUnitType(UnitType.HOLDING_UNIT);
    return unit;
  }

  private static String getSupportedExtension(String str) {
    return DroidUtils.isSupportedExtension(str) ? str : null;
  }

  public abstract void parse(String sedaVersion, Path manifestPath, Path sipDir) throws IOException;

  public abstract ArchiveTransfer getArchiveTransfert();

  public abstract List<DataObjectGroup> getDataObjectGroups();

  public abstract List<ArchiveUnit> getArchiveUnits();

  public abstract ManagementMetadata getManagementMetadata();

  protected void checkAttachedUnitType(UnitType childType, UnitType parentType) {
    if ((childType == UnitType.HOLDING_UNIT && parentType != UnitType.HOLDING_UNIT)
        || (childType == UnitType.FILING_UNIT && parentType == UnitType.INGEST)) {

      throw new ManifestException(
          "Check attached unit failed",
          String.format("Attaching '%s' to '%s' is not allowed", childType, parentType));
    }
  }

  protected Long getAgreementLinkId() {
    checkUpdateOperationIsNotRequired();
    Long linkId = ingestContractDb.getLinkParentId();
    return linkId == null ? ROOT : linkId;
  }

  protected ArchiveUnit getLinkUnit(Long systemId) throws IOException {
    checkUpdateOperationIsAllowed();

    if (systemId.equals(ROOT)) {
      checkParentIds(rootUnit);
      return rootUnit;
    }

    ArchiveUnit linkUnit = linkSystemIdMap.get(systemId);
    if (linkUnit == null) {
      linkUnit = searchService.getArchiveUnit(tenant, systemId);
      checkParentIds(linkUnit);
      linkSystemIdMap.put(systemId, linkUnit);
    }
    return linkUnit;
  }

  protected ArchiveUnit getLinkUnit(String metadataName, String metadataValue) throws IOException {
    checkUpdateOperationIsAllowed();

    Key<String, String> key = new Key<>(metadataName, metadataValue);
    ArchiveUnit linkUnit = linkKeyMap.get(key);
    if (linkUnit == null) {
      linkUnit = searchService.getArchiveUnit(tenant, key);
      checkParentIds(linkUnit);
      linkKeyMap.put(key, linkUnit);
    }
    return linkUnit;
  }

  private void checkUpdateOperationIsAllowed() {
    assertNotNull(
        ingestContractDb,
        PARENTS_FAILED,
        "An ingest contract must be declared before attaching archive unit");

    if (ingestContractDb.getCheckParentLink() == CheckParentLinkStatus.UNAUTHORIZED) {
      throw new ManifestException(
          PARENTS_FAILED,
          String.format(
              "Ingest contract '%s' does not allow update operation",
              ingestContractDb.getIdentifier()));
    }
  }

  private void checkUpdateOperationIsNotRequired() {
    assertNotNull(
        ingestContractDb,
        PARENTS_FAILED,
        "An ingest contract must be declared before attaching archive unit");

    if (ingestContractDb.getCheckParentLink() == CheckParentLinkStatus.REQUIRED) {
      throw new ManifestException(
          PARENTS_FAILED,
          String.format(
              "Ingest contract '%s' requires an update operation",
              ingestContractDb.getIdentifier()));
    }
  }

  // Check the link unit given in the manifest by update operation
  protected void checkParentIds(ArchiveUnit linkUnit) {
    Set<Long> checkParentIds = ingestContractDb.getCheckParentIds();

    if (checkParentIds.isEmpty() || checkParentIds.contains(linkUnit.getId())) {
      return;
    }

    if (linkUnit != rootUnit) {
      for (Long parentId : linkUnit.getParentIds()) {
        if (checkParentIds.contains(parentId)) {
          return;
        }
      }
    }

    throw new ManifestException(
        "Failed to check update operation",
        String.format(
            "The parent unit id '%s' is not allowed in Ingest Contract '%s'",
            linkUnit.getId(), ingestContractDb.getIdentifier()));
  }

  // Check Agency exists and actif
  protected void checkAgency(String identifier) {
    AgencyDb agencyDb =
        agencyMap.computeIfAbsent(identifier, id -> agencyService.getEntity(tenant, id));

    if (agencyDb.getStatus() == Status.INACTIVE) {
      throw new ManifestException(
          "Check agency failed",
          String.format("Agency '%s' is inactive", agencyDb.getIdentifier()));
    }
  }

  // Check Ingest Contract exists and actif
  protected void checkArchivalAgreement(String identifier) {
    ingestContractDb = ingestContractService.getEntity(tenant, identifier);

    if (ingestContractDb.getStatus() == Status.INACTIVE) {
      throw new ManifestException(
          "Check contract agreement failed",
          String.format(
              "Archival Contract Agreement '%s' is inactive", ingestContractDb.getIdentifier()));
    }
  }

  // Check Management Rules
  protected void checkManagementRules(Management management) {
    dateRuleService.computeEndDates(tenant, ruleMap, management);
  }

  // Check Profile belongs to Ingest Contract and exists and actif then validate SEDA
  protected void checkArchivalProfile(String identifier, Path manifestPath) {
    assertNotNull(
        ingestContractDb,
        ARCHIVAL_PROFILE_FAILED,
        String.format(
            "Ingest Contract must be declared before Archive Profile in manifest '%s'",
            manifestPath));

    Set<String> allowedProfiles = ingestContractDb.getArchiveProfiles();

    if (allowedProfiles.isEmpty() && StringUtils.isBlank(identifier)) {
      return;
    }

    if (StringUtils.isBlank(identifier)) {
      throw new ManifestException(
          ARCHIVAL_PROFILE_FAILED,
          String.format(
              "Archive Profile is required by Ingest Contract '%s'",
              ingestContractDb.getIdentifier()));
    }

    if (!allowedProfiles.contains(identifier)) {
      throw new ManifestException(
          ARCHIVAL_PROFILE_FAILED,
          String.format(
              "Archive Profile '%s' is not allowed in Ingest Contract '%s'",
              identifier, ingestContractDb.getIdentifier()));
    }

    // It's not necessary to cache profile because it's used at most one time per sip
    ProfileDb profileDb = profileService.getEntity(tenant, identifier);

    if (profileDb.getStatus() == Status.INACTIVE) {
      throw new ManifestException(
          ARCHIVAL_PROFILE_FAILED,
          String.format("Archive Profile '%s' is inactive", profileDb.getIdentifier()));
    }

    if (profileDb.getFormat() != ProfileFormat.RNG) {
      throw new ManifestException(
          ARCHIVAL_PROFILE_FAILED,
          String.format(
              "Archive profile '%s' format '%s' is not supported",
              profileDb.getIdentifier(), profileDb.getFormat()));
    }

    ByteContent content = profileService.getProfileData(tenant, profileDb.getIdentifier());
    byte[] binaryData = content.bytes();

    if (binaryData == null || binaryData.length == 0) {
      throw new ManifestException(
          ARCHIVAL_PROFILE_FAILED,
          String.format("Archive profile data '%s' is empty", profileDb.getIdentifier()));
    }
    try {
      Validator rngValidator = Validators.getRngValidator(binaryData);
      Sedav2Validator.validate(manifestPath, rngValidator);
    } catch (SAXException | IOException ex) {
      throw new ManifestException(
          ARCHIVAL_PROFILE_FAILED,
          String.format(
              "Unable to validate manifest with Archive Profile '%s'", profileDb.getIdentifier()),
          ex);
    }
  }

  // Check BinaryDataObject/Uri exists
  protected void checkMaxArchiveUnits(int num) {
    if (num > UNIT_MAX) {
      throw new ManifestException(
          "Check max archive units failed",
          String.format("The number of units in the archive is larger than %d", UNIT_MAX));
    }
  }

  // Check BinaryDataObject
  protected void checkBinary(Path binaryPath) {
    if (Files.notExists(binaryPath) || Files.isDirectory(binaryPath)) {
      throw new ManifestException(
          "Check binary failed", String.format("Binary object %s does not exist", binaryPath));
    }
    Path parentPath = binaryPath.getParent();
    if (!"Content".equals(parentPath.getFileName().toString())) {
      throw new ManifestException(
          "Check binary failed",
          String.format("Binary object folder '%s' is not valid", parentPath));
    }
  }

  // Check Binary Size
  protected void checkBinarySize(Path binaryPath, long binarySize) throws IOException {
    if (binarySize != 0 && Files.size(binaryPath) != binarySize) {
      throw new ManifestException(
          "Check binary size failed",
          String.format("Binary object size of '%s' is not valid", binaryPath));
    }
  }

  // Check Binary Digest
  protected void checkBinaryDigest(Path binaryPath, String digestAlgorithm, String binaryDigest)
      throws IOException {

    if (!HashUtils.isValidDigest(digestAlgorithm)) {
      throw new ManifestException(
          "Check binary digest failed",
          String.format("Digest algorithm '%s' is not supported", digestAlgorithm));
    }

    String digest = SipUtils.digestHex(binaryPath, digestAlgorithm);
    if (!digest.equals(binaryDigest)) {
      throw new ManifestException(
          "Check binary digest failed",
          String.format("Binary object digest of '%s' is not valid", binaryPath));
    }
  }

  // Check Binary Size
  protected void checkBinaryFormat(
      Path binaryPath, FormatIdentification formatIdentification, FileInfo fileInfo) {

    assertNotNull(
        ingestContractDb,
        BINARY_FORMAT_FAILED,
        "Ingest Contract must be declared before binary format identification");
    String formatName = formatIdentification.getFormatName();
    String formatId = formatIdentification.getFormatId();

    // Best effort mode!
    String extension = null;
    if (fileInfo != null) {
      extension = getSupportedExtension(FilenameUtils.getExtension(fileInfo.getFilename()));
    }
    if (extension == null) {
      extension = getSupportedExtension(FilenameUtils.getExtension(binaryPath.toString()));
    }
    if (extension == null) {
      extension = getSupportedExtension(formatName);
    }
    if (extension == null) {
      extension = getSupportedExtension(formatId);
    }

    // Providing extension could improve binary signature matching performance
    List<IdentificationResult> results = DroidUtils.matchBinarySignatures(binaryPath, extension);
    if (!results.isEmpty()) {
      IdentificationResult idr = results.getFirst();
      if (!idr.getPuid().equals(formatId)) {
        log.warn("Binary object format id '{}' does not match '{}'", idr.getPuid(), formatId);
        formatIdentification.setFormatId(idr.getPuid());
      }

      formatIdentification.setFormatName(idr.getName() != null ? idr.getName() : "");
      formatIdentification.setFormatLitteral(idr.getVersion() != null ? idr.getVersion() : "");
      formatIdentification.setMimeType(idr.getMimeType() != null ? idr.getMimeType() : "");
    }

    boolean isEveryFormatType = ingestContractDb.getEveryFormatType();

    if (isEveryFormatType) {
      boolean isAuthorizeUnidentified = ingestContractDb.getFormatUnidentifiedAuthorized();
      if (!isAuthorizeUnidentified && results.isEmpty()) {
        throw new ManifestException(
            BINARY_FORMAT_FAILED,
            String.format(
                "Binary format '%s' is unidentified for ingest contract '%s'",
                extension, ingestContractDb.getIdentifier()));
      }
    } else {
      Set<String> allowedFormats = ingestContractDb.getFormatType();
      if (allowedFormats != null && !allowedFormats.contains(formatIdentification.getFormatId())) {
        throw new ManifestException(
            BINARY_FORMAT_FAILED,
            String.format(
                "Binary format '%s' is not an allowed format for ingest contract '%s'",
                formatIdentification.getFormatName(), ingestContractDb.getIdentifier()));
      }
    }
  }

  // Check Physical Qualifier
  protected void checkPhysicalVersion(String version, List<PhysicalDataObject> pdos) {
    if (StringUtils.isBlank(version)) {
      throw new ManifestException(
          PHYSICAL_VERSION_FAILED, String.format("Physical version '%s' is empty", version));
    }
    if (!SipUtils.startsWithPhysicalQualifier(version)) {
      throw new ManifestException(
          PHYSICAL_VERSION_FAILED, String.format("Unknown Physical qualifier '%s'", version));
    }
    if (pdos.size() > 1) {
      throw new ManifestException(
          PHYSICAL_VERSION_FAILED,
          String.format("Physical version '%s' greater than 1 is not allowed", version));
    }
  }

  // Check Binary Qualifier (Qualifier_Version)
  protected String checkBinaryVersion(String binaryVersion, List<BinaryDataObject> bdos) {
    if (StringUtils.isBlank(binaryVersion)) {
      throw new ManifestException(
          BINARY_VERSION_FAILED, String.format("Binary version '%s' is empty", binaryVersion));
    }

    if (!SipUtils.startsWithBinaryQualifier(binaryVersion)) {
      throw new ManifestException(
          BINARY_VERSION_FAILED,
          String.format("Not valid qualifier for binary version '%s'", binaryVersion));
    }

    String[] tokens = StringUtils.split(binaryVersion, '_');
    if (tokens.length > 2) {
      throw new ManifestException(
          BINARY_VERSION_FAILED, String.format("Not valid binary version '%s'", binaryVersion));
    }

    if (tokens.length == 2) {
      if (!NumberUtils.isParsable(tokens[1])) {
        throw new ManifestException(
            BINARY_VERSION_FAILED,
            String.format(
                "Version '%s' is not valid for binary version '%s'", tokens[1], binaryVersion));
      }

      int version = Integer.parseInt(tokens[1]);
      if (version < 0 || version > 999999) {
        throw new ManifestException(
            BINARY_VERSION_FAILED,
            String.format(
                "Version '%d' is not valid for binary version '%s'", version, binaryVersion));
      }

      for (BinaryDataObject bdo : bdos) {
        if (binaryVersion.equals(bdo.getBinaryVersion())) {
          throw new ManifestException(
              BINARY_VERSION_FAILED,
              String.format("Binary version '%s' already exists", binaryVersion));
        }
      }
      return binaryVersion;
    }

    // Version is not specified. We take the greatest version plus one (default is 1)
    int version = 0;
    for (BinaryDataObject bdo : bdos) {
      String bv = bdo.getBinaryVersion();
      if (bv != null) {
        String[] tks = StringUtils.split(bv, '_');
        if (binaryVersion.equals(tks[0])) {
          version = Math.max(version, Integer.parseInt(tks[1]));
        }
      }
    }
    return binaryVersion + "_" + (version + 1);
  }

  protected void checkOntologyKey(OntologyMap ontologyMap, String srcKey, String value) {
    if (ontologyMap != null) {
      String dstKey = ontologyMap.get(srcKey);
      if (StringUtils.isNotBlank(dstKey)) {
        checkKey(dstKey, value);
        return;
      }
    }
    checkKey(srcKey, value);
  }

  private static void checkKey(String key, String value) {
    Field field = ArchiveUnitIndex.INSTANCE.getField(key);
    assertFalse(
        field == null || !field.isValid(value),
        "Failed to check field in archive unit",
        String.format("Unknown or invalid field with key '%s' - value '%s'", key, value));
  }

  protected static void assertNotNull(Object obj, String title, String message) {
    if (obj == null) throw new ManifestException(title, message);
  }

  protected static void assertNull(Object obj, String title, String message) {
    if (obj != null) throw new ManifestException(title, message);
  }

  protected static void assertFalse(boolean b, String title, String message) {
    if (b) throw new ManifestException(title, message);
  }

  protected static void assertTrue(boolean b, String title, String message) {
    if (!b) throw new ManifestException(title, message);
  }
}
