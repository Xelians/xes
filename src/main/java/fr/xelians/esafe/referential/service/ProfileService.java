/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.utils.ByteContent;
import fr.xelians.esafe.common.utils.DroidUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.ProfileFormat;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.dto.ProfileDto;
import fr.xelians.esafe.referential.entity.ProfileDb;
import fr.xelians.esafe.referential.repository.ProfileRepository;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
public class ProfileService extends AbstractReferentialService<ProfileDto, ProfileDb> {

  public static final String PROFILE_DATA_NOT_FOUND = "Profile data not found";
  public static final String PROFILE_DATA_EMPTY = "Profile data is empty";
  public static final String PROFILE_DATA_UNKNOWN = "Profile data cannot be identified";
  public static final String PROFILE_DATA_NOT_VALID = "Profile data is not valid";

  public static final String XSD_PUID = "x-fmt/280";
  public static final String XML_PUID = "fmt/101";

  @Autowired
  public ProfileService(
      EntityManager entityManager,
      ProfileRepository repository,
      OperationService operationService,
      TenantService tenantService) {
    super(entityManager, repository, operationService, tenantService);
  }

  @Override
  public ProfileDto toDto(ProfileDb entity) {
    ProfileDto dto = Utils.copyProperties(entity, createDto());
    if (entity.getData() != null) {
      String base = StringUtils.defaultIfBlank(entity.getName(), entity.getIdentifier());
      dto.setPath((base + "." + entity.getFormat()).toLowerCase());
    }
    return dto;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<ProfileDto> createProfiles(
      OperationDb operation, Long tenant, List<ProfileDto> dtos) {
    OperationDb op = saveOperation(operation);
    return super.create(op, tenant, dtos);
  }

  @Transactional(rollbackFor = Exception.class)
  public ProfileDto updateProfile(
      OperationDb operation, Long tenant, String identifier, ProfileDto dto) {
    OperationDb op = saveOperation(operation);
    return super.update(op, tenant, identifier, dto);
  }

  @Transactional(rollbackFor = Exception.class)
  public void updateDataByIdentifier(
      OperationDb operation, Long tenant, String identifier, byte[] data) throws IOException {
    Assert.hasText(identifier, "identifier must not be null or empty");
    Assert.notNull(data, "data must not be null");

    OperationDb op = saveOperation(operation);
    ProfileFormat format = getProfileFormat(data, identifier);

    ProfileDb profile = getEntity(tenant, identifier);
    profile.setFormat(format);
    profile.setData(data);
    profile.setLastUpdate(LocalDate.now());
    op.setProperty01(profile.getId().toString());
    repository.save(profile);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteProfile(OperationDb operation, Long tenant, String identifier) {
    // TODO Check that profile in not yet used & remove profile binary
    OperationDb op = saveOperation(operation);
    super.delete(op, tenant, identifier);
  }

  private ProfileFormat getProfileFormat(byte[] data, String identifier) throws IOException {
    if (data.length == 0) {
      throw new BadRequestException(
          PROFILE_DATA_EMPTY,
          String.format("Profile validation data is empty for profile '%s'", identifier));
    }

    List<IdentificationResult> results = DroidUtils.matchBinarySignatures(data, null);
    for (IdentificationResult idr : results) {
      String puid = idr.getPuid();

      // Check for xsd (xsd has greater priority over xml)
      if (XSD_PUID.equals(puid)) {
        validateXml(data, identifier);
        return ProfileFormat.XSD;
      }
      // Check for xml (unfortunately rng is not defined in the pronom registry)
      if (XML_PUID.equals(puid)) {
        validateXml(data, identifier);
        return ProfileFormat.RNG;
      }
    }
    throw new NotFoundException(
        PROFILE_DATA_UNKNOWN,
        String.format("Failed to identify profile validation data for profile '%s'", identifier));
  }

  private void validateXml(byte[] data, String identifier) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.parse(new InputSource(new ByteArrayInputStream(data)));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new BadRequestException(
          PROFILE_DATA_NOT_VALID,
          String.format("Profile validation data is not a valid xml for profile '%s'", identifier));
    }
  }

  public ByteContent getProfileData(Long tenant, String identifier) {
    ProfileDb profile = getEntity(tenant, identifier);
    byte[] data = profile.getData();
    if (data == null) {
      throw new NotFoundException(
          PROFILE_DATA_NOT_FOUND,
          String.format(
              "Profile validation (RNG/XSD) data not found for profile '%s'", identifier));
    }

    String ext = "." + profile.getFormat().toString().toLowerCase();
    String name = StringUtils.defaultIfBlank(profile.getName(), profile.getIdentifier());
    name = StringUtils.appendIfMissingIgnoreCase(name, ext);
    return new ByteContent(name, data);
  }

  public SearchResult<ProfileDto> search(Long tenant, SearchQuery query) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "query must be not null");
    return search(ReferentialParser.createProfileParser(tenant, entityManager), query);
  }

  @Override
  protected String getIdentifierPrefix() {
    return "PR";
  }
}
