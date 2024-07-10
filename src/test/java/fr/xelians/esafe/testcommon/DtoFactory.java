/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.testcommon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.xelians.esafe.archive.domain.ingest.Mapping;
import fr.xelians.esafe.archive.domain.ingest.sedav2.ontology.XamOntology;
import fr.xelians.esafe.archive.domain.search.update.JsonPatchBuilder;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.operation.dto.vitam.VitamExternalEventDto;
import fr.xelians.esafe.organization.domain.role.GlobalRole;
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.referential.domain.ProfileFormat;
import fr.xelians.esafe.referential.domain.RuleMeasurement;
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.dto.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DtoFactory {

  private static final AtomicLong counter = new AtomicLong();
  private static final ObjectMapper mapper;

  static {
    mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static String unique() {
    return String.valueOf(counter.incrementAndGet());
  }

  // SignupDto
  public static SignupDto createSignupDto() {
    SignupDto signupDto = new SignupDto();
    signupDto.setOrganizationDto(createOrganisationDto());
    signupDto.setUserDto(createUserDto());
    signupDto.setTenantDto(createTenantDto());
    return signupDto;
  }

  public static OrganizationDto createOrganisationDto() {
    return createOrganisationDto(unique());
  }

  public static OrganizationDto createOrganisationDto(String identifier) {
    OrganizationDto organizationDto = new OrganizationDto();
    organizationDto.setIdentifier(identifier);
    organizationDto.setName("My Organization Name");
    organizationDto.setDescription("My Organization Description");
    organizationDto.setStatus(Status.ACTIVE);
    return organizationDto;
  }

  // TenantDto
  public static TenantDto createTenantDto() {
    TenantDto tenantDto = new TenantDto();
    tenantDto.setName("My Tenant");
    tenantDto.setDescription("My Tenant Description");
    tenantDto.setStorageOffers(new ArrayList<>(List.of("S3:minio01", "FS:FS01")));
    //        tenantDto.setStorageOffers(new ArrayList<>(List.of("FS:FS01")));
    tenantDto.setEncrypted(false);
    tenantDto.setStatus(Status.ACTIVE);
    return tenantDto;
  }

  public static TenantDto createTenantDto(String... offers) {
    TenantDto tenantDto = new TenantDto();
    tenantDto.setName("My Tenant");
    tenantDto.setDescription("My Tenant Description");
    tenantDto.setStorageOffers(new ArrayList<>(Arrays.asList(offers)));
    tenantDto.setEncrypted(false);
    tenantDto.setStatus(Status.ACTIVE);
    return tenantDto;
  }

  // UserDto
  public static UserDto createUserDto(String identifier) {
    UserDto userDto = new UserDto();
    userDto.setIdentifier(identifier);
    userDto.setName("Jean Dupont");
    userDto.setUsername("jean.dupont_" + identifier);
    userDto.setPassword(("mypassword"));
    userDto.setFirstName("Jean");
    userDto.setLastName("Dupond");
    userDto.setEmail("jean.dupont_" + identifier + "@mymail.com");
    userDto.setStatus(Status.ACTIVE);
    userDto.getGlobalRoles().add(GlobalRole.ROLE_ADMIN);
    return userDto;
  }

  public static UserDto createUserDto() {
    return createUserDto(unique());
  }

  // Access Rule
  public static RuleDto createAccessRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("ACCESSRULE-" + str);
    ruleDto.setName("NAME-" + str);
    ruleDto.setDescription("Description " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.AccessRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  // Appraisal Rule
  public static RuleDto createAppraisalRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("APPRAISALRULE-" + str);
    ruleDto.setName("NAME-" + str);
    ruleDto.setDescription("Description " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.AppraisalRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  public static RuleDto createReuseRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("REUSERULE-" + str);
    ruleDto.setName("NAME-" + str);
    ruleDto.setDescription("Description :  " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.ReuseRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  public static RuleDto createStorageRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("STORAGERULE-" + str);
    ruleDto.setName("STORAGENAME-" + str);
    ruleDto.setDescription("Description :  " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.StorageRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  public static RuleDto createClassificationRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("CLASSIFICATIONRULE-" + str);
    ruleDto.setName("NAME-" + str);
    ruleDto.setDescription("Description :  " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.ClassificationRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  public static RuleDto createDisseminationRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("DISSEMINATIONRULE-" + str);
    ruleDto.setName("NAME-" + str);
    ruleDto.setDescription("Description :  " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.DisseminationRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  public static RuleDto createHoldRule(int n) {
    String str = TestUtils.pad(n);
    RuleDto ruleDto = new RuleDto();
    ruleDto.setIdentifier("HOLDRULE-" + str);
    ruleDto.setName("NAME-" + str);
    ruleDto.setDescription("Description :  " + str);
    ruleDto.setStatus(Status.ACTIVE);
    ruleDto.setType(RuleType.HoldRule);
    ruleDto.setDuration("10");
    ruleDto.setMeasurement(RuleMeasurement.YEAR);
    return ruleDto;
  }

  // Access Contract
  public static AccessContractDto createAccessContractDto(int n) {
    String str = TestUtils.pad(n);

    AccessContractDto accessContractDto = new AccessContractDto();
    accessContractDto.setIdentifier("AC-" + str);
    accessContractDto.setName("NAME-" + n);
    accessContractDto.setDescription("DESCRIPTION " + str);
    accessContractDto.setWritingPermission(true);
    accessContractDto.setWritingRestrictedDesc(true);
    accessContractDto.setStatus(Status.ACTIVE);
    accessContractDto.setActivationDate(LocalDate.of(1969, Month.FEBRUARY, 27));
    accessContractDto.setDeactivationDate(LocalDate.of(2070, Month.DECEMBER, 1));
    return accessContractDto;
  }

  // Ingest Contract
  public static IngestContractDto createIngestContractDto(int n) {
    String str = TestUtils.pad(n);
    IngestContractDto ingestContractDto = new IngestContractDto();
    ingestContractDto.setIdentifier("IC-" + str);
    ingestContractDto.setName("NAME-" + n);
    ingestContractDto.setDescription("DESCRIPTION " + str);
    ingestContractDto.setStatus(Status.ACTIVE);
    return ingestContractDto;
  }

  public static IngestContractDto createIngestContractDto(String identifier) {
    IngestContractDto ingestContractDto = new IngestContractDto();
    ingestContractDto.setIdentifier(identifier);
    ingestContractDto.setName("NAME-" + identifier);
    ingestContractDto.setDescription("DESCRIPTION " + identifier);
    ingestContractDto.setStatus(Status.ACTIVE);
    return ingestContractDto;
  }

  // AgencyDto
  public static AgencyDto createAgencyDto(int n) {
    String str = TestUtils.pad(n);
    AgencyDto agencyDto = new AgencyDto();
    agencyDto.setIdentifier("AGENCY-" + str);
    agencyDto.setName("NAME-" + n);
    agencyDto.setDescription("DESCRIPTION " + str);
    agencyDto.setStatus(Status.ACTIVE);
    return agencyDto;
  }

  public static AgencyDto createAgencyDto(String identifier) {
    AgencyDto agencyDto = new AgencyDto();
    agencyDto.setIdentifier(identifier);
    agencyDto.setName("NAME-" + identifier);
    agencyDto.setDescription("DESCRIPTION " + identifier);
    agencyDto.setStatus(Status.ACTIVE);
    return agencyDto;
  }

  // ProfileDto
  public static ProfileDto createProfileDto(int n) {
    String str = TestUtils.pad(n);
    ProfileDto profileDto = new ProfileDto();
    profileDto.setIdentifier("PROFILE-" + str);
    profileDto.setName("NAME-" + n);
    profileDto.setDescription("DESCRIPTION " + str);
    profileDto.setFormat(ProfileFormat.RNG);
    profileDto.setStatus(Status.ACTIVE);
    return profileDto;
  }

  public static ProfileDto createProfileDto(String identifier) {
    ProfileDto profileDto = new ProfileDto();
    profileDto.setIdentifier(identifier);
    profileDto.setName("NAME-" + identifier);
    profileDto.setDescription("DESCRIPTION " + identifier);
    profileDto.setStatus(Status.ACTIVE);
    profileDto.setFormat(ProfileFormat.RNG);
    return profileDto;
  }

  // IndexMapDto
  public static List<OntologyDto> createOntologyDtos(Path path) {
    try {
      return mapper.readValue(path.toFile(), new TypeReference<>() {});
    } catch (IOException e) {
      throw new InternalException("IndexMap creation failed", "Failed with exception:", e);
    }
  }

  public static OntologyDto createOntologyDto(String identifier) {
    OntologyDto ontologyDto = new OntologyDto();
    ontologyDto.setIdentifier("DOCTYPE-" + identifier);
    ontologyDto.setName("NAME-" + identifier);
    ontologyDto.setDescription("DESCRIPTION " + identifier);
    ontologyDto.setStatus(Status.ACTIVE);

    List<Mapping> mappings = new ArrayList<>(XamOntology.MAPPINGS);
    mappings.add(new Mapping("Directeur.Nom", "Keyword145"));
    mappings.add(new Mapping("Directeur.Prenom", "Text197"));
    mappings.add(new Mapping("Directeur.Age", "Long010"));
    mappings.add(new Mapping("Code", "Double015"));
    mappings.add(new Mapping("Codes.Code", "Double016"));

    ontologyDto.setMappings(mappings);
    return ontologyDto;
  }

  public static OntologyDto createOntologyDto(int n) {
    String str = TestUtils.pad(n);
    return createOntologyDto(str);
  }

  public static VitamExternalEventDto createExternalOperationDto() {

    VitamExternalEventDto event = new VitamExternalEventDto();
    event.setEventType("EXT_UPDATE_USER");
    event.setOutcome("OK");
    event.setOutcomeDetailMessage("L'utilisateur a été mis à jour avec succès");
    event.setEventTypeProcess("EXTERNAL_LOGBOOK");
    event.setObjectIdentifier("12678");
    event.setObjectIdentifierRequest("users");
    event.setEventDetailData(
        "{\"diff\":{\"-Nom\":\"-\",\"+Nom\":\"-\"},\"Date d'opération\":\"2023-11-22T10:07:52.519\"}");
    event.setEventDateTime(LocalDateTime.now());
    event.setEventIdentifierRequest("65d12f8f-bc60-416d-a724-1b4080905858");

    VitamExternalEventDto parentEvent = new VitamExternalEventDto();
    parentEvent.setEventIdentifier("aecaaaaabkhy4jnaaaqjqaml62abvpyaaaaq");
    parentEvent.setEventType("EXT_VITAMUI_UPDATE_USER");
    parentEvent.setEventIdentifierProcess("aecaaaaabkhy4jnaaaqjqaml62abvpyaaaaq");
    parentEvent.setEventTypeProcess("EXTERNAL_LOGBOOK");
    parentEvent.setOutcome("STARTED");
    parentEvent.setOutcomeDetailMessage("La création de l'utilisateur a été démarrée");
    parentEvent.getEvents().add(event);

    return parentEvent;
  }

  // ArchiveUnit
  public static ArchiveUnit createSmallUnit(String systemId) {
    ArchiveUnit unit = new ArchiveUnit();
    unit.setId(Long.parseLong(systemId));
    unit.setParentId(1000L);
    unit.setParentIds(Arrays.asList(1000L, 1001L, 1002L, 1003L));
    unit.setTitle("The Title");
    unit.setDescription(("The Description"));
    unit.setDocumentType("The documentType");
    unit.addKeyValue("key1", "value1");
    unit.addKeyValue("key2", "value3");
    unit.addKeyValue("key3", "value3");
    return unit;
  }

  public static JsonNode createJsonPatch() {
    return new JsonPatchBuilder()
        .add("/ArchivalAgencyArchiveUnitIdentifier", "Patched Agency")
        .replace("/Title", "Patched Title")
        .build();
  }

  public static JsonNode createJsonPatch2() {
    return new JsonPatchBuilder().replace("/Title", "Patched Title v2").build();
  }
}
