/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.testcommon;

import fr.xelians.esafe.archive.domain.unit.DescriptionLevel;
import fr.xelians.sipg.model.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SipFactory {

  public static ArchiveTransfer createHolding(int n) {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement("IC-" + TestUtils.pad(n));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    ArchiveUnit unit1 = new ArchiveUnit();
    unit1.setId("HOLDING_ID1");
    unit1.addTitle("Holding_Directory_1");

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("HOLDING_ID2");
    unit2.addTitle("Holding_Directory_2");

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("HOLDING_ID3");
    unit3.addTitle("Holding_Directory_3");

    unit2.addArchiveUnit(unit3);
    unit1.addArchiveUnit(unit2);
    archiveTransfer.addArchiveUnit(unit1);
    return archiveTransfer;
  }

  public static ArchiveTransfer createSmallSip(Path tmpDir, int n) throws IOException {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement("IC-" + TestUtils.pad(n));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    Path binaryPath1 = tmpDir.resolve("hellolsimplesip_1.pdf");
    TestUtils.createPdf("Hello Simple Sip 1", binaryPath1);

    ArchiveUnit unit1 = new ArchiveUnit();
    unit1.setId("UNIT_ID1");
    unit1.addTitle("MyTitle1");
    unit1.setBinaryPath(binaryPath1);
    unit1.addTitle("MyTitle1");
    unit1.setStatus("Indexed");
    unit1.addDescription("MyDescription1");
    unit1.addTag("Keyword001", "MyValue1");

    Path binaryPath2 = tmpDir.resolve("hellolsimplesip_2.pdf");
    TestUtils.createPdf("Hello Simple Sip 2", binaryPath2);

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setBinaryPath(binaryPath2);
    unit2.setBinaryVersion("BinaryMaster_1");
    unit2.addTitle("MyTitle2");
    unit2.addDescription("MyDescription2");
    unit2.addTag("Keyword002", "MyValue2");

    Path binaryPath3 = tmpDir.resolve("hellolsimplesip_3.pdf");
    TestUtils.createPdf("Hello Simple Sip 3", binaryPath3);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath3);
    unit3.setBinaryVersion("BinaryMaster");
    unit3.addTitle("MyTitle3");
    unit3.addDescription("MyDescription3");
    unit3.addTag("Keyword003", "MyValue3");

    archiveTransfer.addArchiveUnit(unit1);
    archiveTransfer.addArchiveUnit(unit2);
    archiveTransfer.addArchiveUnit(unit3);
    return archiveTransfer;
  }

  public static ArchiveTransfer createSimpleSip(Path tmpDir, int n) throws IOException {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement("IC-" + TestUtils.pad(n));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    ArchiveUnit unit1 = new ArchiveUnit();
    unit1.setId("UNIT_ID1");
    unit1.addTitle("MyTitle1");

    Path binaryPath2 = tmpDir.resolve("hellolsimplesip_2.pdf");
    TestUtils.createPdf("Hello Simple Sip 2", binaryPath2);

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setBinaryPath(binaryPath2);
    unit2.setBinaryVersion("BinaryMaster_2");
    unit2.addTitle("MyTitle2");
    unit2.addDescription("MyDescription2");
    unit2.addTag("Keyword002", "MyValue2");
    unit1.addArchiveUnit(unit2);

    Path binaryPath3 = tmpDir.resolve("hellolsimplesip_3.pdf");
    TestUtils.createPdf("Hello Simple Sip 3", binaryPath3);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath3);
    unit3.setBinaryVersion("BinaryMaster_3");
    unit3.addTitle("MyTitle3");
    unit3.addDescription("MyDescription3");
    unit3.addTag("Keyword003", "MyValue3");
    unit1.addArchiveUnit(unit3);

    archiveTransfer.addArchiveUnit(unit1);
    return archiveTransfer;
  }

  public static ArchiveTransfer createSimpleSipWithNowDate(Path tmpDir, int n) throws IOException {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement("IC-" + TestUtils.pad(n));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    ArchiveUnit unit1 = new ArchiveUnit();
    unit1.setId("UNIT_ID1");
    unit1.addTitle("MyTitle1");
    unit1.setDocumentType("The unknown document type");
    unit1.addElement("<Language>French</Language>");
    unit1.addElement("<DescriptionLanguage>FR</DescriptionLanguage>");

    Path binaryPath2 = tmpDir.resolve("hellolsimplesip_2.pdf");
    TestUtils.createPdf("Hello Simple Sip 2", binaryPath2);

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setBinaryPath(binaryPath2);
    unit2.setBinaryVersion("BinaryMaster");
    unit2.addTitle("MyTitle2");
    unit2.addDescription("MyDescription2");
    unit2.addTag("Keyword002", "MyValue2");

    LocalDate startDate = LocalDate.now();
    AppraisalRules aRule2 = new AppraisalRules();
    aRule2.addRule("APPRAISALRULE-" + TestUtils.pad(1), startDate);
    aRule2.setFinalAction("Destroy");
    unit2.setAppraisalRules(aRule2);
    unit1.addArchiveUnit(unit2);

    Path binaryPath3 = tmpDir.resolve("hellolsimplesip_3.pdf");
    TestUtils.createPdf("Hello Simple Sip 3", binaryPath3);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath3);
    unit3.setBinaryVersion("BinaryMaster");
    unit3.addTitle("MyTitle3");
    unit3.addDescription("MyDescription3");
    unit3.addTag("Keyword003", "MyValue3");
    unit1.addArchiveUnit(unit3);

    archiveTransfer.addArchiveUnit(unit1);
    return archiveTransfer;
  }

  public static ArchiveTransfer createSimpleSipWithPastDate(Path tmpDir, int n) throws IOException {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement("IC-" + TestUtils.pad(n));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    ArchiveUnit unit1 = new ArchiveUnit();
    unit1.setId("UNIT_ID1");
    unit1.addTitle("MyTitle1");
    unit1.setDescriptionLanguage("De");
    unit1.addLanguage("Fr");

    Path binaryPath2 = tmpDir.resolve("hellolsimplesip_2.pdf");
    TestUtils.createPdf("Hello Simple Sip 2", binaryPath2);

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setBinaryPath(binaryPath2);
    unit2.setBinaryVersion("BinaryMaster");
    unit2.addTitle("MyTitle2");
    unit2.addDescription("MyDescription2");
    unit2.addTag("Keyword002", "MyValue2");
    unit1.addArchiveUnit(unit2);

    Path binaryPath3 = tmpDir.resolve("hellolsimplesip_3.pdf");
    TestUtils.createPdf("Hello Simple Sip 3", binaryPath3);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath3);
    unit3.setBinaryVersion("BinaryMaster");
    unit3.addTitle("MyTitle3");
    unit3.addDescription("MyDescription3");
    //      unit3.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit3.addTag("Keyword003", "MyValue3");

    LocalDate startDate = LocalDate.now().minusYears(15);
    AppraisalRules aRule3 = new AppraisalRules();
    aRule3.addRule("APPRAISALRULE-" + TestUtils.pad(1), startDate);
    aRule3.setFinalAction("Destroy");
    unit3.setAppraisalRules(aRule3);
    unit1.addArchiveUnit(unit3);

    archiveTransfer.addArchiveUnit(unit1);
    return archiveTransfer;
  }

  public static ArchiveTransfer createUpdateOperationSimpleSip(Path tmpDir, int n)
      throws IOException {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement(("IC-" + TestUtils.pad(n)));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    ArchiveUnit unit1 = new ArchiveUnit();
    UpdateOperation updateOperation = new UpdateOperation("Keyword002", "MyValue2");
    unit1.setUpdateOperation(updateOperation);

    Path binaryPath2 = tmpDir.resolve("hellolupdatesip_1.pdf");
    TestUtils.createPdf("Hello Update Sip 1", binaryPath2);

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setBinaryPath(binaryPath2);
    unit2.addTitle("MyUpdateTitle2");
    unit2.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit1.setDescriptionLanguage("English");
    unit1.addLanguage("EN");
    unit2.addTag("Keyword002", "MyValue2");
    unit1.addArchiveUnit(unit2);

    Path binaryPath3 = tmpDir.resolve("hellolupdatesip_2.pdf");
    TestUtils.createPdf("Hello Update Sip 2", binaryPath3);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath3);
    unit3.setFormatName("pdf");
    unit3.addTitle("MyUpdateTitle3");
    unit3.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit3.addTag("Keyword003", "MyValue3");
    unit1.addArchiveUnit(unit3);

    archiveTransfer.addArchiveUnit(unit1);
    return archiveTransfer;
  }

  public static ArchiveTransfer createUpdateOperationSip(Path tmpDir, int n, long systemId)
      throws IOException {
    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setArchivalAgreement(("IC-" + TestUtils.pad(n)));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    ArchiveUnit unit1 = new ArchiveUnit();
    UpdateOperation updateOperation = new UpdateOperation(String.valueOf(systemId));
    unit1.setUpdateOperation(updateOperation);

    Path binaryPath2 = tmpDir.resolve("hellolupdatesip_1.pdf");
    TestUtils.createPdf("Hello Update Sip 1", binaryPath2);

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setBinaryPath(binaryPath2);
    unit2.addTitle("MyTitle2");
    unit2.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit1.setDescriptionLanguage("English");
    unit1.addLanguage("EN");
    unit2.addTag("Keyword002", "MyValue2");
    unit1.addArchiveUnit(unit2);

    Path binaryPath3 = tmpDir.resolve("hellolupdatesip_2.pdf");
    TestUtils.createPdf("Hello Update Sip 2", binaryPath3);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath3);
    unit3.setFormatName("pdf");
    unit3.addTitle("MyTitle3");
    unit3.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit3.addTag("Keyword003", "MyValue3");
    unit1.addArchiveUnit(unit3);

    archiveTransfer.addArchiveUnit(unit1);
    return archiveTransfer;
  }

  public static ArchiveTransfer createComplexSip(Path tmpDir, int n) throws IOException {

    CodeListVersions clvs = new CodeListVersions();
    clvs.setFileFormatCodeListVersion("Pronom Codes");
    clvs.setReplyCodeListVersion("Reply Codes");

    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setMessageIdentifier("MSG001");
    archiveTransfer.setDate(LocalDateTime.now());
    archiveTransfer.setComment("My Archive Transfer");
    archiveTransfer.setSignature("org.afnor.signformat.PKCS-7");
    archiveTransfer.setCodeListVersions(clvs);
    archiveTransfer.setArchivalAgreement(("IC-" + TestUtils.pad(n)));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");
    archiveTransfer.setOriginatingAgencyIdentifier("OriginatingAgencyId");
    archiveTransfer.setSubmissionAgencyIdentifier("SubmissionAgencyId");
    // archiveTransfer.setArchivalProfile("My Archival Profile");
    archiveTransfer.setLegalStatus("Public Archive");
    archiveTransfer.setServiceLevel("My Service Level");

    Path binaryPath1 = tmpDir.resolve("hello_binary_complexsip_1.pdf");
    TestUtils.createPdf("Hello Binary Complex Sip 1", binaryPath1);

    Path dissPath1 = tmpDir.resolve("hello_diss_complexsip_1.pdf");
    TestUtils.createPdf("Hello Dissemination Complex Sip 1", dissPath1);

    Path thumbPath1 = tmpDir.resolve("hello_thum_complexsip_1.pdf");
    TestUtils.createPdf("Hello Thumbnail Complex Sip 1", thumbPath1);

    Path textPath1 = tmpDir.resolve("hello_text_complexsip_1.pdf");
    TestUtils.createPdf("Hello Text Content Complex Sip 1", textPath1);

    Path binaryPath2 = tmpDir.resolve("hellolcomplexsip_2.pdf");
    TestUtils.createPdf("Hello Complex Sip 2", binaryPath2);

    ArchiveUnit unit1 = new ArchiveUnit();
    unit1.setId("UNIT_ID1");
    unit1.setPhysicalId("physical-0001");
    unit1.setPhysicalVersion("PhysicalMaster");
    unit1.setDescriptionLevel(DescriptionLevel.File.toString());
    unit1.setMeasure(26);
    unit1.setVersion("Version1");
    unit1.setStatus("Indexed");
    unit1.setType("Type 7");

    LocalDate startDate = LocalDate.now().minusYears(15);

    AppraisalRules aRule1 = new AppraisalRules();
    aRule1.addRule("APPRAISALRULE-" + TestUtils.pad(1), startDate);
    aRule1.addRule("APPRAISALRULE-" + TestUtils.pad(2), startDate);
    aRule1.addRule("APPRAISALRULE-" + TestUtils.pad(3), startDate);
    aRule1.setPreventInheritance(true);
    aRule1.setFinalAction("Destroy");
    unit1.setAppraisalRules(aRule1);

    StorageRules sRule1 = new StorageRules();
    sRule1.addRule("STORAGERULE-" + TestUtils.pad(1), startDate);
    sRule1.addRule("STORAGERULE-" + TestUtils.pad(2), startDate);
    sRule1.addRule("STORAGERULE-" + TestUtils.pad(3), startDate);
    sRule1.setPreventInheritance(false);
    sRule1.setFinalAction("Copy");
    unit1.setStorageRules(sRule1);

    DisseminationRules dRule1 = new DisseminationRules();
    dRule1.addRule("DISSEMINATIONRULE-" + TestUtils.pad(1), startDate);
    dRule1.addRule("DISSEMINATIONRULE-" + TestUtils.pad(2), startDate);
    dRule1.addRule("DISSEMINATIONRULE-" + TestUtils.pad(3), startDate);
    dRule1.addPreventRuleName("RuleName4");
    dRule1.addPreventRuleName("RuleName5");
    unit1.setDisseminationRules(dRule1);

    ReuseRules rRule1 = new ReuseRules();
    rRule1.addRule("REUSERULE-" + TestUtils.pad(1), startDate);
    rRule1.addRule("REUSERULE-" + TestUtils.pad(2), startDate);
    rRule1.addRule("REUSERULE-" + TestUtils.pad(3), startDate);
    rRule1.addPreventRuleName("RuleName4");
    rRule1.addPreventRuleName("RuleName5");
    unit1.setReuseRules(rRule1);

    // LocalDate today = LocalDate.now();
    LocalDate day = LocalDate.now().minusDays(1);

    HoldRules hRule1 = new HoldRules();
    hRule1.addRule("HOLDRULE-" + TestUtils.pad(1), day, day, "MySelf1", "Maybe1", day, false);
    hRule1.addRule("HOLDRULE-" + TestUtils.pad(2), day, day, "MySelf2", null, day, false);
    hRule1.addRule("HOLDRULE-" + TestUtils.pad(3), day, day, "MySelf3", "Maybe3", null, false);
    hRule1.addPreventRuleName("RuleName r4");
    hRule1.addPreventRuleName("RuleName r5");
    unit1.setHoldRules(hRule1);

    LocalDateTime now = LocalDateTime.now();

    Event event1 =
        EventBuilder.builder()
            .withDateTime(now)
            .withDetail("MyDetails1")
            .withDetailData("MyDetailsData1")
            .withIdentifier("MyIdentifier1")
            .withOutcome("MyOutcome1")
            .withOutcomeDetail("MyOutcomeDetail1")
            .withType("MyType1")
            .withTypeCode("MyTypeCode1")
            .build();

    Event event2 =
        EventBuilder.builder()
            .withDateTime(now)
            .withDetail("MyDetails2")
            .withDetailData("MyDetailsData2")
            .withIdentifier("MyIdentifier2")
            .withOutcome("MyOutcome2")
            .withOutcomeDetail("MyOutcomeDetail2")
            .withType("MyType2")
            .withTypeCode("MyTypeCode2")
            .build();

    Event event3 =
        EventBuilder.builder()
            .withDateTime(now)
            .withDetail("MyDetails3")
            .withDetailData("MyDetailsData3")
            .withIdentifier("MyIdentifier3")
            .withOutcome("MyOutcome3")
            .withOutcomeDetail("MyOutcomeDetail3")
            .withType("MyType3")
            .withTypeCode("MyTypeCode3")
            .build();

    unit1.addLogEvent(event1);
    unit1.addLogEvent(event2);
    unit1.addLogEvent(event3);

    unit1.setDocumentType("UNKNOWN_USE_DEFAULT_DOCTYPE-" + TestUtils.pad(1));

    unit1.addCustodialItem("My Message1", now);
    unit1.addCustodialItem("My Message2", now);

    unit1.addOriginatingSystemId("000001");
    unit1.addTitle("MyTitle1");
    unit1.addDescription("My Description of first Archive Unit");
    unit1.setCreatedDate(LocalDate.of(2021, 1, 11));
    unit1.setStartDate(LocalDate.of(2021, 1, 12));
    unit1.setEndDate(LocalDate.of(2026, 1, 11));
    unit1.setSentDate(LocalDate.of(2023, 11, 21));

    unit1.addElement(new Element("PhysicalType", "Boite"));
    unit1.addElement(new Element("PhysicalStatus", "En Stock"));
    unit1.addElement(new Element("PhysicalBarcode", "1782JUY9087J"));
    unit1.addElement(new Element("PhysicalAgency", "Dexto"));
    unit1.addElement(new Element("PhysicalIdentifier", "23589APO"));
    // unit1.addElement(new Element("DuaStartDate", "2024-07-25"));

    unit1.addTag("Keyword011", "MyValue11");
    unit1.addTag("Keyword012", "MyValue12");
    unit1.addTag("MyValue13");

    unit1.addTag("position_type", "SERIAL");
    unit1.addTag("position_enabled", "true");

    Agency oriAgency = new Agency("AGENCY-" + TestUtils.pad(1), "My Transfer Agency");
    oriAgency.addElement("Address", "Rue de la Jarry - Vincennes");
    unit1.setOriginatingAgencyIdentifier(oriAgency.getIdentifier());
    unit1.setSubmissionAgencyIdentifier(oriAgency.getIdentifier());

    unit1.addAddressee(
        AgentBuilder.builder()
            .withFirstName("Marc")
            .withFullName("Lavolle")
            .withBirthDate(startDate)
            .addActivity("Sword")
            .addFunction("Jedi")
            .withBirthPlace(
                PlaceBuilder.builder()
                    .withAddress("19 Holliday Street")
                    .withGeogName("GEOName")
                    .withPostalCode("94300")
                    .withRegion("Oregon")
                    .withCountry("USA")
                    .withCity("NY")
                    .build())
            .build());

    unit1.addTransmitter(
        AgentBuilder.builder()
            .withFirstName("Joël")
            .withFullName("Bruneau")
            .withBirthDate(startDate)
            .addActivity("LP")
            .addFunction("Super")
            .withBirthPlace(
                PlaceBuilder.builder()
                    .withAddress("189 Mars Allée")
                    .withGeogName("GEOName2")
                    .withPostalCode("93800")
                    .withRegion("Ile de France")
                    .withCountry("France")
                    .withCity("Beauchamp")
                    .build())
            .build());

    unit1.setSource("My Source1");

    ArchiveUnit unit2 = new ArchiveUnit();
    unit2.setId("UNIT_ID2");
    unit2.setPhysicalId("physical-0002");
    unit2.setVersion("Version2");
    unit2.setPhysicalVersion("PhysicalMaster");
    unit2.setMeasure(236);
    unit2.setDescriptionLevel(DescriptionLevel.Item.toString());
    unit2.setStatus("Stored");

    unit2.setBinaryPath(binaryPath1);
    unit2.setFormatName("pdf");
    unit2.setFileInfo(
        FileInfoBuilder.builder()
            .withFilename("MyBinaryMasterFile.pdf")
            .withLastModified(now)
            .withCreatingApplicationName("PdfBox")
            .withCreatingOs("Linux")
            .withCreatingOsVersion("Ubuntu 22.10")
            .withCreatingApplicationVersion("3.32.1")
            .withDateCreatedByApplication(LocalDateTime.now())
            .build());
    // unit2.setSignatureStatus("No Signature");

    unit2.setDisseminationPath(dissPath1);
    unit2.setDisseminationFormatName("Portable Document Format");
    unit2.setDisseminationFileInfo(
        FileInfoBuilder.builder()
            .withFilename("MyDisseminationFile.pdf")
            .withLastModified(now)
            .withCreatingApplicationName("PdfBoxDissemination")
            .withCreatingOs("Linux")
            .withCreatingOsVersion("Ubuntu 22.10")
            .withCreatingApplicationVersion("3.32.1")
            .withDateCreatedByApplication(LocalDateTime.now())
            .build());

    unit2.setThumbnailPath(thumbPath1);
    unit2.setThumbnailVersion("Thumbnail_2");
    unit2.setThumbnailFormatName("Portable Document Format");
    unit2.setThumbnailFileInfo(
        FileInfoBuilder.builder()
            .withFilename("MyThumbnailFile.pdf")
            .withLastModified(now)
            .withCreatingApplicationName("PdfBoxThumbnail")
            .withCreatingOs("Linux")
            .withCreatingOsVersion("Ubuntu 22.10")
            .withCreatingApplicationVersion("3.32.1")
            .withDateCreatedByApplication(LocalDateTime.now())
            .build());

    unit2.setTextContentPath(textPath1);
    unit2.setTextContentVersion("TextContent_1");
    unit2.setTextContentFormatId("fmt/18");
    unit2.setTextContentFormatName("Portable Document Format");
    unit2.setTextContentFileInfo(
        FileInfoBuilder.builder()
            .withFilename("MyTextContentFile.pdf")
            .withLastModified(now)
            .withCreatingApplicationName("PdfBoxTextContent")
            .withCreatingOs("Linux")
            .withCreatingOsVersion("Ubuntu 22.10")
            .withCreatingApplicationVersion("3.32.1")
            .withDateCreatedByApplication(LocalDateTime.now())
            .build());

    DisseminationRules dRule2 = new DisseminationRules();
    dRule2.addRule("DISSEMINATIONRULE-" + TestUtils.pad(1), startDate);
    unit2.setDisseminationRules(dRule2);

    AccessRules aRule2 = new AccessRules();
    aRule2.addRule("ACCESSRULE-" + TestUtils.pad(1), startDate);
    aRule2.addRule("ACCESSRULE-" + TestUtils.pad(2), startDate);
    aRule2.addRule("ACCESSRULE-" + TestUtils.pad(3), startDate);
    aRule2.addPreventRuleName("RuleName4");
    aRule2.addPreventRuleName("RuleName5");
    unit2.setAccessRules(aRule2);

    ClassificationRules cRule2 = new ClassificationRules();
    cRule2.addRule("CLASSIFICATIONRULE-" + TestUtils.pad(1), startDate);
    cRule2.addRule("CLASSIFICATIONRULE-" + TestUtils.pad(2), startDate);
    cRule2.addRule("CLASSIFICATIONRULE-" + TestUtils.pad(3), startDate);
    cRule2.addPreventRuleName("RuleName4");
    cRule2.addPreventRuleName("RuleName5");
    cRule2.setClassificationAudience("EveryOne");
    cRule2.setClassificationOwner("Captain Deviller");
    cRule2.setClassificationLevel("TOP SECRET");
    cRule2.setClassificationReassessingDate(startDate);
    cRule2.setNeedReassessingAuthorization(Boolean.FALSE);
    unit2.setClassificationRules(cRule2);

    unit2.setGpsAltitude("60");
    unit2.setGpsLatitude("48.8534");
    unit2.setGpsLongitude("2.3488");

    unit2.setArchiveUnitProfile("My Archive Unit Profile 2");

    unit2.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit2.addOriginatingSystemId("000002");
    unit2.addTitle("MyTitle2");
    unit2.addDescription("My Description of second Archive Unit");
    unit2.setCreatedDate(LocalDate.of(2022, 2, 12));

    unit2.addTag("Keyword012", "MyValue12");
    unit2.addTag("Keyword013", "MyValue13");
    unit2.addTag("MyValue23");

    unit2.addAuthorizedAgent(
        AgentBuilder.builder()
            .withIdentifiers(List.of("A12456"))
            .withFirstName("Jacques")
            .withFullName("Terner")
            .withBirthDate(startDate)
            .withBirthName("Jacky Ho")
            .withGivenName("Joe")
            .withGender("Male")
            .withDeathDate(startDate)
            .addActivity("Controller")
            .addFunction("BOSS")
            .addPosition("High")
            .addNationality("French")
            .addRole("SmallBoss")
            .addMandate("Mandataire")
            .withBirthPlace(
                PlaceBuilder.builder()
                    .withAddress("MyAddress")
                    .withGeogName("LND")
                    .withRegion("Sussex")
                    .withPostalCode("98765")
                    .withCountry("England")
                    .withCity("London")
                    .build())
            .withDeathPlace(
                PlaceBuilder.builder()
                    .withAddress("MyAddress")
                    .withGeogName("PRS")
                    .withRegion("Seine")
                    .withPostalCode("75012")
                    .withCountry("France")
                    .withCity("Paris")
                    .build())
            .build());

    unit2.addAuthorizedAgent(
        AgentBuilder.builder()
            .withIdentifiers(List.of("AB1756"))
            .withFirstName("Pierre")
            .withFullName("Ulanut")
            .withBirthDate(startDate)
            .withBirthName("Pierrot Terrade")
            .withGivenName("Pierrot")
            .withGender("Male")
            .withDeathDate(startDate)
            .addActivity("Footballer")
            .addFunction("Back")
            .addPosition("Low")
            .addNationality("Spain")
            .addRole("SmallBoss")
            .addMandate("Mandataire")
            .withBirthPlace(
                PlaceBuilder.builder()
                    .withAddress("The Address")
                    .withGeogName("LYT")
                    .withRegion("Sax")
                    .withPostalCode("98765")
                    .withCountry("Germany")
                    .withCity("Berlin")
                    .build())
            .withDeathPlace(
                PlaceBuilder.builder()
                    .withAddress("The Address")
                    .withGeogName("PRS")
                    .withRegion("Seine")
                    .withPostalCode("75012")
                    .withCountry("France")
                    .withCity("Paris")
                    .build())
            .build());

    unit2.addWriter(
        AgentBuilder.builder()
            .withIdentifiers(List.of("B536378"))
            .withFirstName("Emmanuel")
            .withFullName("Deviller")
            .withBirthDate(startDate)
            .addActivity("Author of this code")
            .addActivity("Developper")
            .addFunction("CTO")
            .withBirthPlace(PlaceBuilder.builder().withCountry("France").withCity("Paris").build())
            .build());

    unit2.addWriter(
        AgentBuilder.builder()
            .withIdentifiers(List.of("C435296"))
            .withFirstName("Galatée")
            .withFullName("Deviller")
            .withBirthDate(startDate)
            .addActivity("Consultant")
            .addFunction("Chat")
            .withBirthPlace(
                PlaceBuilder.builder().withCountry("Germany").withCity("Baden-Baden").build())
            .build());

    unit2.addAddressee(
        AgentBuilder.builder()
            .withIdentifiers(List.of("D87656"))
            .withFirstName("Marc")
            .withFullName("Lavolle")
            .withBirthDate(startDate)
            .addActivity("Sword")
            .addFunction("Jedi")
            .withBirthPlace(PlaceBuilder.builder().withCountry("USA").withCity("NY").build())
            .build());

    unit2.addRecipient(
        AgentBuilder.builder()
            .withIdentifiers(List.of("EH654432"))
            .withFirstName("Tom")
            .withFullName("Johns")
            .withBirthDate(startDate)
            .addActivity("Sword")
            .addFunction("Guerrier")
            .withBirthPlace(PlaceBuilder.builder().withCountry("USA").withCity("NY").build())
            .build());

    unit2.addTransmitter(
        AgentBuilder.builder()
            .withIdentifiers(List.of("F432626"))
            .withFirstName("Jacques")
            .withFullName("Garel")
            .withBirthDate(startDate)
            .addActivity("Lance")
            .addFunction("Magicien")
            .withBirthPlace(
                PlaceBuilder.builder().withCountry("Espagne").withCity("Madrid").build())
            .build());

    unit2.addSender(
        AgentBuilder.builder()
            .withIdentifiers(List.of("G6512456"))
            .withFirstName("Ben")
            .withFullName("Targatien")
            .withBirthDate(startDate)
            .addActivity("Dague")
            .addFunction("Voleur")
            .withBirthPlace(
                PlaceBuilder.builder().withCountry("Royaume-Uni").withCity("Londres").build())
            .build());

    Element e1 = new Element("Directeur");
    e1.addElement(new Element("Nom", "Deviller"));
    e1.addElement(new Element("Prenom", "Emmanuel"));
    e1.addElement(new Element("Age", "78"));
    unit2.addElement(e1);

    unit2.addElement(new Element("Code", "94000"));
    unit2.addElement(new Element("Code", "94001"));
    unit2.addElement(new Element("Code", "94002"));

    unit2.addElement("<Codes><Code>1</Code><Code>2</Code><Code>3</Code></Codes>");

    RelatedObjectRef ror = new RelatedObjectRef();
    ror.addVersionOf(new ArchiveUnitRef(unit1));
    ror.addVersionOf(new DataObjectRef(unit1));
    ror.addVersionOf(new RepositoryArchiveUnitPID("repo archive pid"));
    ror.addVersionOf(new RepositoryObjectPID("repo object pid"));
    ror.addVersionOf(new ExternalReference("Test external ref"));
    unit2.setRelation(ror);

    unit1.addArchiveUnit(unit2);

    ArchiveUnit unit3 = new ArchiveUnit();
    unit3.setId("UNIT_ID3");
    unit3.setBinaryPath(binaryPath2);
    unit3.setFormatName("pdf");
    unit3.setVersion("Version3");
    // unit3.setSignatureStatus("No Signature");
    unit3.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
    unit3.addOriginatingSystemId("000003");
    unit3.addTitle("MyTitle3");
    unit3.setCreatedDate(LocalDate.of(2023, 3, 13));

    unit3.addTag("Keyword014", "MyValue14");
    unit3.addTag("MyValue17");
    unit3.setPhysicalId("physical-0001");

    AppraisalRules aRule3 = new AppraisalRules();
    aRule3.addRule("APPRAISALRULE-" + TestUtils.pad(1), startDate.plusYears(1));
    aRule3.addRule("APPRAISALRULE-" + TestUtils.pad(2), startDate.plusYears(3));
    aRule3.setPreventInheritance(true);
    aRule3.setFinalAction("Destroy");
    unit3.setAppraisalRules(aRule3);

    Signer signer =
        SignerBuilder.builder()
            .withFirstName("Marc")
            .withFullName("Lavolle")
            .withBirthDate(startDate)
            .addActivity("Sword")
            .addFunction("Jedi")
            .addMandate("Signer")
            .withBirthPlace(PlaceBuilder.builder().withCountry("USA").withCity("NY").build())
            .withSigningTime(now)
            .build();

    Validator validator =
        ValidatorBuilder.builder()
            .withFirstName("Marc")
            .withFullName("Lavolle")
            .withBirthDate(startDate)
            .addActivity("Sword")
            .addFunction("Jedi")
            .addMandate("Validator")
            .withBirthPlace(PlaceBuilder.builder().withCountry("USA").withCity("NY").build())
            .withValidationTime(now)
            .build();

    Signature signature = new Signature();
    signature.addSigner(signer);
    signature.setValidator(validator);
    //      unit3.addSignature(signature);

    RelatedObjectRef relation = new RelatedObjectRef();
    relation.addRequire(new ArchiveUnitRef(unit1));
    relation.addPartOf(new ExternalReference("ExternalRef"));
    relation.addReference(new RepositoryArchiveUnitPID("RepoArcUnitPid"));
    relation.addReplace(new DataObjectRef(unit2));
    unit3.setRelation(relation);

    unit1.addArchiveUnit(unit3);

    archiveTransfer.addArchiveUnit(unit1);

    return archiveTransfer;
  }

  public static ArchiveTransfer createLargeSip(Path tmpDir, int n, int m) throws IOException {
    LocalDate today = LocalDate.now();
    LocalDateTime todaytime = LocalDateTime.now();

    ArchiveTransfer archiveTransfer = new ArchiveTransfer();
    archiveTransfer.setMessageIdentifier("MSG001");
    archiveTransfer.setDate(todaytime);
    archiveTransfer.setComment("My Archive Transfer");
    archiveTransfer.setArchivalAgreement("IC-" + TestUtils.pad(n));
    archiveTransfer.setArchivalAgency("AGENCY-" + TestUtils.pad(n), "Archival Agency");
    archiveTransfer.setTransferringAgency("AGENCY-" + TestUtils.pad(n), "Transferring Agency");

    String uuid = UUID.randomUUID().toString();
    Path binaryPath = tmpDir.resolve("hellolargesip_" + uuid + ".pdf");
    TestUtils.createPdf("Hello Large Sip ", binaryPath);

    for (int i = 0; i < m; i++) {

      ArchiveUnit unit = new ArchiveUnit();
      unit.setId("UNIT_ID" + (i + 1));
      unit.setBinaryPath(binaryPath);
      unit.setSignatureStatus("No Signature");

      unit.setAccessRules("ACCESSRULE-" + TestUtils.pad(n), today);
      unit.setAppraisalRules("APPRAISALRULE-" + TestUtils.pad(n), today);
      unit.getAppraisalRules().setFinalAction("Destroy");

      unit.setDocumentType("DOCTYPE-" + TestUtils.pad(1));
      unit.addOriginatingSystemId(String.valueOf(i));
      unit.addTitle("MyTitle " + i);
      unit.addTag("Keyword001", "MyValue1_" + i);
      unit.addTag("Keyword002", "MyValue2_" + i);
      unit.addTag("MyValue3");

      archiveTransfer.addArchiveUnit(unit);
    }
    return archiveTransfer;
  }
}
