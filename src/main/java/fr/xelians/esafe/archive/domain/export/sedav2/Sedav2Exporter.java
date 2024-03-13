/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.export.sedav2;

import static fr.xelians.esafe.common.constant.Sedav2.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.export.ExportConfig;
import fr.xelians.esafe.archive.domain.export.Exporter;
import fr.xelians.esafe.archive.domain.search.export.DataObjectVersionToExport;
import fr.xelians.esafe.archive.domain.search.export.DipExportType;
import fr.xelians.esafe.archive.domain.search.export.DipRequestParameters;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.object.*;
import fr.xelians.esafe.archive.domain.unit.object.FileInfo;
import fr.xelians.esafe.archive.domain.unit.object.FormatIdentification;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
import fr.xelians.esafe.archive.domain.unit.rules.management.AbstractRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.AccessRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.AppraisalRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.ClassificationRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.DisseminationRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.HoldRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.ReuseRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.StorageRules;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.sipg.model.*;
import fr.xelians.sipg.model.Agency;
import fr.xelians.sipg.model.RelatedObjectRef;
import fr.xelians.sipg.service.sedav2.Sedav2Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;

public class Sedav2Exporter implements Exporter {

  public static final String KEY_IS_NOT_A_VALUE_NODE = "Key %s is not a value node";

  private final Long tenant;
  private final StorageDao storageDao;
  private final List<String> storageOffers;
  private final ExportConfig exportConfig;

  public Sedav2Exporter(
      Long tenant, List<String> storageOffers, StorageDao storageDao, ExportConfig exportConfig) {
    this.tenant = tenant;
    this.storageOffers = storageOffers;
    this.storageDao = storageDao;
    this.exportConfig = exportConfig;
  }

  public void export(List<ArchiveUnit> srcUnits, Path path) throws IOException {

    DipRequestParameters params = exportConfig.dipRequestParameters();

    fr.xelians.sipg.model.ArchiveTransfer archiveTransfer =
        new fr.xelians.sipg.model.ArchiveTransfer();
    archiveTransfer.setArchivalAgreement(params.archivalAgreement());
    archiveTransfer.setOriginatingAgencyIdentifier(params.originatingAgencyIdentifier());
    archiveTransfer.setSubmissionAgencyIdentifier(params.submissionAgencyIdentifier());
    archiveTransfer.setArchivalAgency(params.archivalAgencyIdentifier(), null);
    archiveTransfer.setTransferringAgency(params.archivalAgencyIdentifier(), null);
    archiveTransfer.setMessageIdentifier(params.messageRequestIdentifier());
    archiveTransfer.setComment(params.comment());

    for (ArchiveUnit srcUnit : srcUnits) {
      fr.xelians.sipg.model.ArchiveUnit dstUnit = toArchiveUnit(srcUnit);
      archiveTransfer.addArchiveUnit(dstUnit);
      visitArchiveUnits(srcUnit, dstUnit);
    }

    Sedav2Service.getV22Instance().write(archiveTransfer, path);
  }

  // Convert to SEDA
  private void visitArchiveUnits(ArchiveUnit srcUnit, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    for (ArchiveUnit srcChildUnit : srcUnit.getChildUnitMap().values()) {
      fr.xelians.sipg.model.ArchiveUnit dstChildUnit = toArchiveUnit(srcChildUnit);
      dstUnit.addArchiveUnit(dstChildUnit);
      visitArchiveUnits(srcChildUnit, dstChildUnit);
    }
  }

  private fr.xelians.sipg.model.ArchiveUnit toArchiveUnit(ArchiveUnit srcUnit) {
    fr.xelians.sipg.model.ArchiveUnit dstUnit = new fr.xelians.sipg.model.ArchiveUnit();

    DataObjectVersionToExport dov = exportConfig.dataObjectVersionToExport();
    if (dov != null && dov.dataObjectVersions() != null) {
      dov.dataObjectVersions()
          .forEach(qualifier -> copyBinaryDataObjects(srcUnit, dstUnit, qualifier));
    }
    copyPhysicalDataObjects(srcUnit, dstUnit);
    copyManagement(srcUnit.getManagement(), dstUnit);

    dstUnit.setDescriptionLevel(srcUnit.getDescriptionLevel());
    dstUnit.addTitle(srcUnit.getTitle());
    dstUnit.addDescription(srcUnit.getDescription());
    dstUnit.setType(srcUnit.getType());
    dstUnit.setDocumentType(srcUnit.getDocumentType());
    srcUnit.getFilePlanPositions().forEach(dstUnit::addFilePlanPosition);
    dstUnit.addSystemId(srcUnit.getId().toString());
    srcUnit.getOriginatingSystemIds().forEach(dstUnit::addOriginatingSystemId);
    srcUnit
        .getOriginatingAgencyArchiveUnitIdentifiers()
        .forEach(dstUnit::addOriginatingAgencyArchiveUnitIdentifier);
    srcUnit
        .getArchivalAgencyArchiveUnitIdentifiers()
        .forEach(dstUnit::addArchivalAgencyArchiveUnitIdentifier);
    srcUnit
        .getTransferringAgencyArchiveUnitIdentifiers()
        .forEach(dstUnit::addTransferringAgencyArchiveUnitIdentifier);
    dstUnit.setStatus(srcUnit.getStatus());
    dstUnit.setVersion(srcUnit.getVersion());
    srcUnit.setArchiveUnitRefId(srcUnit.getArchiveUnitRefId());
    dstUnit.setArchiveUnitProfile(srcUnit.getArchiveUnitProfile());

    srcUnit.getTags().forEach(dstUnit::addTag);
    srcUnit.getKeyTags().forEach(tag -> dstUnit.addTag(tag.key(), tag.value()));
    srcUnit.getCustodialHistoryItems().forEach(i -> dstUnit.addCustodialItem(i, null));

    dstUnit.setAcquiredDate(srcUnit.getAcquiredDate());
    dstUnit.setCreatedDate(srcUnit.getCreatedDate());
    dstUnit.setEndDate(srcUnit.getEndDate());
    dstUnit.setTransactedDate(srcUnit.getTransactedDate());
    dstUnit.setReceivedDate(srcUnit.getReceivedDate());
    dstUnit.setRegisteredDate(srcUnit.getRegisteredDate());
    dstUnit.setSentDate(srcUnit.getSentDate());
    dstUnit.setStartDate(srcUnit.getStartDate());

    copyExtents(srcUnit.getExtents(), dstUnit);

    if (exportConfig.dipExportType() == DipExportType.FULL) {
      String fullText = srcUnit.getFullText();
      if (StringUtils.isNotBlank(fullText)) {
        dstUnit.addElement(new Element(FULL_TEXT, fullText));
      }
    }

    copyLifeCycles(srcUnit, dstUnit);

    return dstUnit;
  }

  private void copyLifeCycles(ArchiveUnit srcUnit, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportConfig.transferWithLogBookLFC() && srcUnit.getLifeCycles() != null) {
      for (LifeCycle lfc : srcUnit.getLifeCycles()) {
        dstUnit.addLogEvent(
            fr.xelians.sipg.model.EventBuilder.builder()
                .withIdentifier(srcUnit.getId() + "_" + lfc.operationId())
                .withDateTime(lfc.operationDate())
                .withType(lfc.operationType().toString())
                .withDetail(lfc.patch())
                .withOutcome(("OK"))
                .build());
      }
    }
  }

  @SneakyThrows
  private Path getBinaryPath(ObjectVersion srcOv) {
    try (InputStream is =
        storageDao.getBinaryObjectStream(
            tenant, storageOffers, srcOv.getOperationId(), srcOv.getPos(), srcOv.getId())) {
      Path path = Files.createTempFile("binary_", ".tmp");
      Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
      return path;
    }
  }

  private void copyBinaryDataObjects(
      ArchiveUnit srcUnit,
      fr.xelians.sipg.model.ArchiveUnit dstUnit,
      BinaryQualifier binaryQualifier) {

    ObjectVersion srcOv =
        Qualifiers.getGreatestObjectVersion(srcUnit.getQualifiers(), binaryQualifier.toString());
    if (srcOv != null) {
      fr.xelians.sipg.model.BinaryDataObject dstBdo =
          dstUnit.provideBinaryDataObject(binaryQualifier.name());

      dstBdo.setBinaryPathSupplier(() -> getBinaryPath(srcOv));
      dstBdo.setBinaryVersion(srcOv.getDataObjectVersion());

      FormatIdentification srcFmt = srcOv.getFormatIdentification();
      dstBdo.getFormatIdentification().setFormatName(srcFmt.getFormatName());
      dstBdo.getFormatIdentification().setFormatId(srcFmt.getFormatId());
      dstBdo.getFormatIdentification().setMimeType(srcFmt.getMimeType());

      FileInfo srcInfo = srcOv.getFileInfo();
      dstBdo.getFileInfo().setFilename(srcInfo.getFilename());
      dstBdo.getFileInfo().setCreatingApplicationName(srcInfo.getCreatingApplicationName());
      dstBdo.getFileInfo().setCreatingApplicationVersion(srcInfo.getCreatingApplicationVersion());
      dstBdo.getFileInfo().setDateCreatedByApplication(srcInfo.getDateCreatedByApplication());
      dstBdo.getFileInfo().setCreatingOs(srcInfo.getCreatingOs());
      dstBdo.getFileInfo().setCreatingOsVersion(srcInfo.getCreatingOsVersion());
      dstBdo.getFileInfo().setLastModified(srcInfo.getLastModified());
    }
  }

  private static void copyPhysicalDataObjects(
      ArchiveUnit srcUnit, fr.xelians.sipg.model.ArchiveUnit dstUnit) {

    ObjectVersion objectVersion =
        Qualifiers.getGreatestObjectVersion(srcUnit.getQualifiers(), "PhysicalMaster");
    if (objectVersion != null) {
      dstUnit.setPhysicalId(objectVersion.getPhysicalId());
      dstUnit.setPhysicalVersion(objectVersion.getDataObjectVersion());
      if (objectVersion.getMeasure() != null) {
        dstUnit.setMeasure(objectVersion.getMeasure());
      }
    }
  }

  private static void copyExtents(JsonNode node, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (node != null) {
      for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = it.next();
        switch (entry.getKey()) {
          case SOURCE -> dstUnit.setSource(asText(entry.getValue()));
          case DESCRIPTION_LANGUAGE -> dstUnit.setDescriptionLanguage(asText(entry.getValue()));
          case LANGUAGE -> getStringArray(node, LANGUAGE).forEach(dstUnit::addLanguage);
          case SIGNATURE_STATUS -> dstUnit.setSignatureStatus(asText(entry.getValue()));
          case ORIGINATING_AGENCY -> dstUnit.setOriginatingAgency(toAgency(entry.getValue()));
          case SUBMISSION_AGENCY -> dstUnit.setSubmissionAgency(toAgency(entry.getValue()));
          case NEED_AUTHORIZATION -> dstUnit.setNeedAuthorization(asBoolean(entry.getValue()));

            // TODO Implement Coverage that is currently not supported in SipG
            //  Coverage.Spatial=text
            //  Coverage.Temporal=text
            //  Coverage.Juridictional=text

            // Authorized Agent & Writing Group
          case AUTHORIZED_AGENT -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toAgent)
              .forEach(dstUnit::addAuthorizedAgent);
          case WRITER -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toAgent)
              .forEach(dstUnit::addWriter);
          case ADDRESSEE -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toAgent)
              .forEach(dstUnit::addAddressee);
          case RECIPIENT -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toAgent)
              .forEach(dstUnit::addRecipient);
          case TRANSMITTER -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toAgent)
              .forEach(dstUnit::addTransmitter);
          case SENDER -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toAgent)
              .forEach(dstUnit::addSender);
          case SIGNATURE -> toNodes(entry.getValue()).stream()
              .map(Sedav2Exporter::toSignature)
              .forEach(dstUnit::addSignature);

            // Gps group
          case GPS -> copyGps(entry.getValue(), dstUnit);

            // Relation Group
          case RELATED_OBJECT_REFERENCE -> dstUnit.setRelation(
              toRelatedObjectRef(entry.getValue()));

            // Extended elements
          default -> {
            Element elt = new Element(entry.getKey());
            dstUnit.addElement(elt);
            copyExtended(entry.getValue(), elt, null, dstUnit);
          }
        }
      }
    }
  }

  private static void copyExtended(
      JsonNode node,
      fr.xelians.sipg.model.Element element,
      fr.xelians.sipg.model.Element parentElt,
      fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (node.isObject()) {
      node.fields()
          .forEachRemaining(
              e -> {
                Element elt = new Element(e.getKey());
                element.addElement(elt);
                copyExtended(e.getValue(), elt, element, dstUnit);
              });
    } else if (node.isArray()) {
      if (!node.isEmpty()) {
        String name = element.getName();
        copyExtended(node.get(0), element, parentElt, dstUnit);
        for (int i = 1; i < node.size(); i++) {
          Element elt = new Element(name);
          if (parentElt == null) {
            dstUnit.addElement(elt);
          } else {
            parentElt.addElement(elt);
          }
          copyExtended(node.get(i), elt, parentElt, dstUnit);
        }
      }
    } else {
      element.setValue(node.asText());
    }
  }

  private static Agency toAgency(JsonNode node) {
    return new Agency(getStringValue(node, IDENTIFIER), getStringValue(node, NAME));
  }

  private static fr.xelians.sipg.model.Signature toSignature(JsonNode node) {
    var dstSignature = new fr.xelians.sipg.model.Signature();
    dstSignature.setDigestAlgorithm(getStringValue(node, DIGEST_ALGORITHM));
    dstSignature.setDigestValue(getStringValue(node, DIGEST_VALUE));
    dstSignature.setValidator(toValidator(node.get(VALIDATOR)));
    toNodes(node.get(SIGNER)).stream()
        .map(Sedav2Exporter::toSigner)
        .forEach(dstSignature::addSigner);
    return dstSignature;
  }

  private static fr.xelians.sipg.model.Signer toSigner(JsonNode node) {
    return node == null
        ? null
        : (fr.xelians.sipg.model.Signer)
            setAgent(new fr.xelians.sipg.model.Signer(getTimeValue(node, SIGNING_TIME)), node);
  }

  private static fr.xelians.sipg.model.Validator toValidator(JsonNode node) {
    return node == null
        ? null
        : (fr.xelians.sipg.model.Validator)
            setAgent(
                new fr.xelians.sipg.model.Validator(getTimeValue(node, VALIDATION_TIME)), node);
  }

  private static fr.xelians.sipg.model.Agent toAgent(JsonNode node) {
    return node == null ? null : setAgent(new fr.xelians.sipg.model.Agent(), node);
  }

  private static Agent setAgent(fr.xelians.sipg.model.Agent dstAgent, JsonNode node) {
    dstAgent.setFirstName(getStringValue(node, FIRST_NAME));
    dstAgent.setFullName(getStringValue(node, FULL_NAME));
    dstAgent.setBirthName(getStringValue(node, BIRTH_NAME));
    dstAgent.setGivenName(getStringValue(node, GIVEN_NAME));
    dstAgent.setGender(getStringValue(node, GENDER));
    dstAgent.setBirthDate(getDateValue(node, BIRTH_DATE));
    dstAgent.setBirthPlace(toPlace(node.get(BIRTH_PLACE)));
    dstAgent.setDeathDate(getDateValue(node, DEATH_DATE));
    dstAgent.setDeathPlace(toPlace(node.get(DEATH_PLACE)));
    dstAgent.setCorpName(getStringValue(node, CORP_NAME));

    getStringArray(node, IDENTIFIER).forEach(dstAgent::addIdentifier);
    getStringArray(node, NATIONALITY).forEach(dstAgent::addNationality);
    getStringArray(node, ACTIVITY).forEach(dstAgent::addActivity);
    getStringArray(node, ROLE).forEach(dstAgent::addRole);
    getStringArray(node, POSITION).forEach(dstAgent::addPosition);
    getStringArray(node, FUNCTION).forEach(dstAgent::addFunction);
    getStringArray(node, MANDATE).forEach(dstAgent::addMandate);

    return dstAgent;
  }

  private static fr.xelians.sipg.model.Place toPlace(JsonNode node) {
    return node == null
        ? null
        : new fr.xelians.sipg.model.Place(
            getStringValue(node, GEOGNAME),
            getStringValue(node, ADDRESS),
            getStringValue(node, POSTAL_CODE),
            getStringValue(node, CITY),
            getStringValue(node, REGION),
            getStringValue(node, COUNTRY));
  }

  private static fr.xelians.sipg.model.RelatedObjectRef toRelatedObjectRef(JsonNode node) {
    fr.xelians.sipg.model.RelatedObjectRef relatedObjectRef = new RelatedObjectRef();
    toNodes(node.get(IS_VERSION_OF)).forEach(e -> setVersionOf(e, relatedObjectRef));
    toNodes(node.get(REPLACES)).forEach(e -> setReplaces(e, relatedObjectRef));
    toNodes(node.get(REQUIRES)).forEach(e -> setRequires(e, relatedObjectRef));
    toNodes(node.get(IS_PART_OF)).forEach(e -> setPartOf(e, relatedObjectRef));
    toNodes(node.get(REFERENCES)).forEach(e -> setReferences(e, relatedObjectRef));
    return relatedObjectRef;
  }

  private static void setVersionOf(
      JsonNode node, fr.xelians.sipg.model.RelatedObjectRef relatedObjectRef) {
    if (node != null) {
      String r0 = getStringValue(node, REPOSITORY_ARCHIVE_UNIT_PID);
      if (r0 != null) {
        relatedObjectRef.addVersionOf(new fr.xelians.sipg.model.RepositoryArchiveUnitPID(r0));
      }
      String r1 = getStringValue(node, REPOSITORY_OBJECT_PID);
      if (r1 != null) {
        relatedObjectRef.addVersionOf(new RepositoryObjectPID(r1));
      }
      String r2 = getStringValue(node, EXTERNAL_REFERENCE);
      if (r2 != null) {
        relatedObjectRef.addVersionOf(new fr.xelians.sipg.model.ExternalReference(r2));
      }
    }
  }

  private static void setReplaces(
      JsonNode node, fr.xelians.sipg.model.RelatedObjectRef relatedObjectRef) {
    if (node != null) {
      String r0 = getStringValue(node, REPOSITORY_ARCHIVE_UNIT_PID);
      if (r0 != null) {
        relatedObjectRef.addReplace(new fr.xelians.sipg.model.RepositoryArchiveUnitPID(r0));
      }
      String r1 = getStringValue(node, REPOSITORY_OBJECT_PID);
      if (r1 != null) {
        relatedObjectRef.addReplace(new RepositoryObjectPID(r1));
      }
      String r2 = getStringValue(node, EXTERNAL_REFERENCE);
      if (r2 != null) {
        relatedObjectRef.addReplace(new fr.xelians.sipg.model.ExternalReference(r2));
      }
    }
  }

  private static void setRequires(
      JsonNode node, fr.xelians.sipg.model.RelatedObjectRef relatedObjectRef) {
    if (node != null) {
      String r0 = getStringValue(node, REPOSITORY_ARCHIVE_UNIT_PID);
      if (r0 != null) {
        relatedObjectRef.addRequire(new fr.xelians.sipg.model.RepositoryArchiveUnitPID(r0));
      }
      String r1 = getStringValue(node, REPOSITORY_OBJECT_PID);
      if (r1 != null) {
        relatedObjectRef.addRequire(new RepositoryObjectPID(r1));
      }
      String r2 = getStringValue(node, EXTERNAL_REFERENCE);
      if (r2 != null) {
        relatedObjectRef.addRequire(new fr.xelians.sipg.model.ExternalReference(r2));
      }
    }
  }

  private static void setPartOf(
      JsonNode node, fr.xelians.sipg.model.RelatedObjectRef relatedObjectRef) {
    if (node != null) {
      String r0 = getStringValue(node, REPOSITORY_ARCHIVE_UNIT_PID);
      if (r0 != null) {
        relatedObjectRef.addPartOf(new fr.xelians.sipg.model.RepositoryArchiveUnitPID(r0));
      }
      String r1 = getStringValue(node, REPOSITORY_OBJECT_PID);
      if (r1 != null) {
        relatedObjectRef.addPartOf(new RepositoryObjectPID(r1));
      }
      String r2 = getStringValue(node, EXTERNAL_REFERENCE);
      if (r2 != null) {
        relatedObjectRef.addPartOf(new fr.xelians.sipg.model.ExternalReference(r2));
      }
    }
  }

  private static void setReferences(
      JsonNode node, fr.xelians.sipg.model.RelatedObjectRef relatedObjectRef) {
    if (node != null) {
      String r0 = getStringValue(node, REPOSITORY_ARCHIVE_UNIT_PID);
      if (r0 != null) {
        relatedObjectRef.addReference(new fr.xelians.sipg.model.RepositoryArchiveUnitPID(r0));
      }
      String r1 = getStringValue(node, REPOSITORY_OBJECT_PID);
      if (r1 != null) {
        relatedObjectRef.addReference(new RepositoryObjectPID(r1));
      }
      String r2 = getStringValue(node, EXTERNAL_REFERENCE);
      if (r2 != null) {
        relatedObjectRef.addReference(new fr.xelians.sipg.model.ExternalReference(r2));
      }
    }
  }

  private static void copyGps(JsonNode node, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    dstUnit.setGpsVersionID(getStringValue(node, GPS_VERSION_ID));
    dstUnit.setGpsAltitude(getStringValue(node, GPS_ALTITUDE));
    dstUnit.setGpsAltitudeRef(getStringValue(node, GPS_ALTITUDE_REF));
    dstUnit.setGpsLongitude(getStringValue(node, GPS_LONGITUDE));
    dstUnit.setGpsLongitudeRef(getStringValue(node, GPS_LONGITUDE_REF));
    dstUnit.setGpsLatitude(getStringValue(node, GPS_LATITUDE));
    dstUnit.setGpsLatitudeRef(getStringValue(node, GPS_LATITUDE_REF));
    dstUnit.setGpsDateStamp(getStringValue(node, GPS_DATE_STAMP));
  }

  private static void copyManagement(Management srcMgt, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (srcMgt != null) {
      copyAccessRules(srcMgt.getAccessRules(), dstUnit);
      copyAppraisalRules(srcMgt.getAppraisalRules(), dstUnit);
      copyDisseminationRules(srcMgt.getDisseminationRules(), dstUnit);
      copyStorageRules(srcMgt.getStorageRules(), dstUnit);
      copyClassificationRules(srcMgt.getClassificationRules(), dstUnit);
      copyReuseRules(srcMgt.getReuseRules(), dstUnit);
      copyHoldRules(srcMgt.getHoldRules(), dstUnit);
    }
  }

  private static void copyReuseRules(
      ReuseRules srcReuseRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcReuseRule)) {
      fr.xelians.sipg.model.ReuseRules dstReuseRules = new fr.xelians.sipg.model.ReuseRules();
      srcReuseRule
          .getRules()
          .forEach(srcRule -> dstReuseRules.addRule(srcRule.getRuleName(), srcRule.getStartDate()));

      RuleInheritance srcRuleInheritance = srcReuseRule.getRuleInheritance();
      dstReuseRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstReuseRules::addPreventRuleName);
      dstUnit.setReuseRules(dstReuseRules);
    }
  }

  private static void copyClassificationRules(
      ClassificationRules srcClassificationRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcClassificationRule)) {
      fr.xelians.sipg.model.ClassificationRules dstClassificationRules =
          new fr.xelians.sipg.model.ClassificationRules();
      srcClassificationRule
          .getRules()
          .forEach(
              srcRule ->
                  dstClassificationRules.addRule(srcRule.getRuleName(), srcRule.getStartDate()));

      RuleInheritance srcRuleInheritance = srcClassificationRule.getRuleInheritance();
      dstClassificationRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstClassificationRules::addPreventRuleName);
      dstClassificationRules.setClassificationAudience(
          srcClassificationRule.getClassificationAudience());
      dstClassificationRules.setClassificationLevel(srcClassificationRule.getClassificationLevel());
      dstClassificationRules.setClassificationOwner(srcClassificationRule.getClassificationOwner());
      dstClassificationRules.setClassificationReassessingDate(
          srcClassificationRule.getClassificationReassessingDate());
      dstClassificationRules.setNeedReassessingAuthorization(
          srcClassificationRule.getNeedReassessingAuthorization());
      dstUnit.setClassificationRules(dstClassificationRules);
    }
  }

  private static void copyHoldRules(
      HoldRules srcHoldRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcHoldRule)) {
      fr.xelians.sipg.model.HoldRules dstHoldRules = new fr.xelians.sipg.model.HoldRules();
      srcHoldRule
          .getHoldRules()
          .forEach(
              srcRule ->
                  dstHoldRules.addRule(
                      srcRule.getRuleName(),
                      srcRule.getStartDate(),
                      srcRule.getHoldEndDate(),
                      srcRule.getHoldOwner(),
                      srcRule.getHoldReason(),
                      srcRule.getHoldReassessingDate(),
                      srcRule.getPreventRearrangement()));

      RuleInheritance srcRuleInheritance = srcHoldRule.getRuleInheritance();
      dstHoldRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstHoldRules::addPreventRuleName);
      dstUnit.setHoldRules(dstHoldRules);
    }
  }

  private static void copyStorageRules(
      StorageRules srcStorageRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcStorageRule)) {
      fr.xelians.sipg.model.StorageRules dstStorageRules = new fr.xelians.sipg.model.StorageRules();
      srcStorageRule
          .getRules()
          .forEach(
              srcRule -> dstStorageRules.addRule(srcRule.getRuleName(), srcRule.getStartDate()));
      dstStorageRules.setFinalAction(srcStorageRule.getFinalAction());

      RuleInheritance srcRuleInheritance = srcStorageRule.getRuleInheritance();
      dstStorageRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstStorageRules::addPreventRuleName);
      dstUnit.setStorageRules(dstStorageRules);
    }
  }

  private static void copyDisseminationRules(
      DisseminationRules srcDisseminationRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcDisseminationRule)) {
      fr.xelians.sipg.model.DisseminationRules dstDisseminationRules =
          new fr.xelians.sipg.model.DisseminationRules();
      srcDisseminationRule
          .getRules()
          .forEach(
              srcRule ->
                  dstDisseminationRules.addRule(srcRule.getRuleName(), srcRule.getStartDate()));

      RuleInheritance srcRuleInheritance = srcDisseminationRule.getRuleInheritance();
      dstDisseminationRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstDisseminationRules::addPreventRuleName);
      dstUnit.setDisseminationRules(dstDisseminationRules);
    }
  }

  private static void copyAppraisalRules(
      AppraisalRules srcAppraisalRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcAppraisalRule)) {
      fr.xelians.sipg.model.AppraisalRules dstAppraisalRules =
          new fr.xelians.sipg.model.AppraisalRules();
      srcAppraisalRule
          .getRules()
          .forEach(
              srcRule -> dstAppraisalRules.addRule(srcRule.getRuleName(), srcRule.getStartDate()));
      dstAppraisalRules.setFinalAction(srcAppraisalRule.getFinalAction());
      dstAppraisalRules.setDuration(srcAppraisalRule.getDuration());

      RuleInheritance srcRuleInheritance = srcAppraisalRule.getRuleInheritance();
      dstAppraisalRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstAppraisalRules::addPreventRuleName);
      dstUnit.setAppraisalRules(dstAppraisalRules);
    }
  }

  private static void copyAccessRules(
      AccessRules srcAccessRule, fr.xelians.sipg.model.ArchiveUnit dstUnit) {
    if (exportRules(srcAccessRule)) {
      fr.xelians.sipg.model.AccessRules dstAccessRules = new fr.xelians.sipg.model.AccessRules();
      srcAccessRule
          .getRules()
          .forEach(
              srcRule -> dstAccessRules.addRule(srcRule.getRuleName(), srcRule.getStartDate()));

      RuleInheritance srcRuleInheritance = srcAccessRule.getRuleInheritance();
      dstAccessRules.setPreventInheritance(srcRuleInheritance.getPreventInheritance());
      srcRuleInheritance.getPreventRulesId().forEach(dstAccessRules::addPreventRuleName);
      dstUnit.setAccessRules(dstAccessRules);
    }
  }

  private static boolean exportRules(AbstractRules arules) {
    return arules != null
        && (!arules.isEmpty()
            || arules.getRuleInheritance().getPreventInheritance() == Boolean.TRUE);
  }

  private static Boolean asBoolean(JsonNode node) {
    return node == null ? null : node.asBoolean();
  }

  private static String asText(JsonNode node) {
    return node == null ? null : node.asText();
  }

  private static String getStringValue(JsonNode node, String key) {
    JsonNode valueNode = node.get(key);
    if (valueNode == null) {
      return null;
    }
    if (valueNode.isValueNode()) {
      return valueNode.asText();
    }
    throw new InternalException(String.format(KEY_IS_NOT_A_VALUE_NODE, key));
  }

  private static LocalDate getDateValue(JsonNode node, String key) {
    JsonNode valueNode = node.get(key);
    if (valueNode == null) {
      return null;
    }
    if (valueNode.isValueNode()) {
      return LocalDate.parse(valueNode.asText());
    }
    throw new InternalException(String.format(KEY_IS_NOT_A_VALUE_NODE, key));
  }

  private static LocalDateTime getTimeValue(JsonNode node, String key) {
    JsonNode valueNode = node.get(key);
    if (valueNode == null) {
      return null;
    }
    if (valueNode.isValueNode()) {
      return LocalDateTime.parse(valueNode.asText());
    }
    throw new InternalException(String.format(KEY_IS_NOT_A_VALUE_NODE, key));
  }

  private static List<String> getStringArray(JsonNode node, String key) {
    JsonNode valueNode = node.get(key);
    if (valueNode == null) {
      return new ArrayList<>();
    }
    if (valueNode.isArray()) {
      List<String> values = new ArrayList<>();
      valueNode.forEach(v -> values.add(v.asText()));
      return values;
    }
    if (valueNode.isValueNode()) {
      List<String> values = new ArrayList<>();
      values.add(valueNode.asText());
      return values;
    }
    throw new InternalException(String.format("Key %s is not an array or a value node", key));
  }

  private static List<JsonNode> toNodes(JsonNode node) {
    if (node == null) {
      return new ArrayList<>();
    }
    if (node.isArray()) {
      List<JsonNode> nodes = new ArrayList<>();
      node.forEach(nodes::add);
      return nodes;
    }
    List<JsonNode> nodes = new ArrayList<>();
    nodes.add(node);
    return nodes;
  }
}
