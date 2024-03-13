/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.organization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.entity.TaskLockDb;
import fr.xelians.esafe.operation.repository.TaskLockRepository;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.domain.role.GlobalRole;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.entity.OrganizationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.repository.TenantRepository;
import fr.xelians.esafe.organization.repository.UserRepository;
import fr.xelians.esafe.storage.domain.Aes;
import fr.xelians.esafe.storage.entity.StorageDb;
import fr.xelians.esafe.storage.repository.StorageRepository;
import fr.xelians.esafe.storage.service.SecretKeyService;
import fr.xelians.esafe.storage.service.StorageService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

  public static final String ENTITY_NAME = "Tenant";
  public static final String NOT_FOUND = "%s with id %s not found";
  public static final String FAILED_TO_CREATE_TENANT = "Failed to create tenant";
  public static final String TENANT_NOT_FOUND = "Tenant not found";

  private final TenantRepository tenantRepository;
  private final StorageRepository storageRepository;
  private final TaskLockRepository taskLockRepository;
  private final UserRepository userRepository;

  private final StorageService storageService;
  private final OrganizationService organizationService;
  private final SecretKeyService secretKeyService;
  private final OperationService operationService;
  private final ApiKeyService apiKeyService;

  public TenantDto toDto(TenantDb entity) {
    TenantDto tenantDto = Utils.copyProperties(entity, new TenantDto());
    tenantDto.setOrganizationIdentifier(entity.getOrganization().getIdentifier());
    return tenantDto;
  }

  public TenantDb toEntity(TenantDto dto) {
    return Utils.copyProperties(dto, new TenantDb());
  }

  @Transactional(rollbackFor = Exception.class)
  public TenantDb createEntity(TenantDto tenantDto, OrganizationDb organizationDb) {
    Assert.notNull(tenantDto, String.format("%s dto cannot be null", ENTITY_NAME));

    checkCreateTenant(tenantDto);

    TenantDb tenantDb = toEntity(tenantDto);
    tenantDb.setOperationId(-1L); // Fake value to avoid null constraint
    tenantDb.setCreationDate(LocalDate.now());
    tenantDb.setLastUpdate(tenantDb.getCreationDate());
    tenantDb.setAutoVersion(1);
    tenantDb.setOrganization(organizationDb);
    TenantDb savedTenantDb = tenantRepository.save(tenantDb);

    final var tenantIdentifier = savedTenantDb.getId();
    storageRepository.save(new StorageDb(tenantIdentifier));
    taskLockRepository.save(new TaskLockDb(tenantIdentifier));
    if (Utils.isTrue(savedTenantDb.getEncrypted())) {
      secretKeyService.saveSecretKey(savedTenantDb.getId(), Aes.createSecretKey());
    }
    return savedTenantDb;
  }

  private void initAdminAccess(TenantDb tenantDb) {
    Long organizationId = tenantDb.getOrganization().getId();
    final var adminUser =
        userRepository.findByOrganizationId(organizationId).stream()
            .filter(user -> user.getGlobalRoles().contains(GlobalRole.ROLE_ADMIN))
            .findFirst()
            .orElseThrow();

    adminUser.getApiKey().add(apiKeyService.buildApiKey(tenantDb.getId().toString()));
    userRepository.save(adminUser);
  }

  @Transactional(rollbackFor = Exception.class)
  public List<TenantDto> create(
      Long organizationId,
      String userIdentifier,
      String applicationId,
      List<TenantDto> tenantDtos) {
    Assert.notNull(tenantDtos, String.format("%s dtos cannot be null", ENTITY_NAME));

    tenantDtos.forEach(this::checkCreateTenant);

    // Create tenant operation
    OrganizationDb organizationDb = organizationService.getOrganizationDbById(organizationId);
    Long tenant = organizationDb.getTenant();
    OperationDb operation = createOperation(tenant, userIdentifier, applicationId);
    List<Long> tenantDbIds = new ArrayList<>(tenantDtos.size());

    List<TenantDto> saveTenantDtos = new ArrayList<>(tenantDtos.size());
    for (TenantDto tenantDto : tenantDtos) {
      TenantDb tenantDb = toEntity(tenantDto);
      tenantDb.setOperationId(operation.getId());
      tenantDb.setAutoVersion(1);
      tenantDb.setCreationDate(operation.getCreated().toLocalDate());
      tenantDb.setLastUpdate(tenantDb.getCreationDate());
      tenantDb.setOrganization(organizationDb);
      TenantDb savedTenantDb = tenantRepository.save(tenantDb);
      initAdminAccess(tenantDb);

      storageRepository.save(new StorageDb(savedTenantDb.getId()));
      taskLockRepository.save(new TaskLockDb(savedTenantDb.getId()));
      if (Utils.isTrue(savedTenantDb.getEncrypted())) {
        secretKeyService.saveSecretKey(savedTenantDb.getId(), Aes.createSecretKey());
      }

      saveTenantDtos.add(toDto(savedTenantDb));
      tenantDbIds.add(savedTenantDb.getId());
    }

    operation.setProperty01(StringUtils.join(tenantDbIds, ','));
    return saveTenantDtos;
  }

  public TenantDto update(
      Long organizationId, String userIdentifier, String applicationId, TenantDto tenantDto) {
    Assert.notNull(tenantDto, "tenant dto cannot be null");
    Assert.notNull(tenantDto.getId(), "tenant cannot be null");
    checkUpdateTenant(tenantDto);

    OrganizationDb organizationDb = organizationService.getOrganizationDbById(organizationId);

    if (tenantDto.getOrganizationIdentifier() != null
        && !organizationDb.getIdentifier().equals(tenantDto.getOrganizationIdentifier())) {
      throw new BadRequestException(
          "Entity update failed",
          String.format(
              "Entity %s identifiers mismatch: %s vs %s",
              ENTITY_NAME, organizationDb.getIdentifier(), tenantDto.getOrganizationIdentifier()));
    }

    // Get tenant from db
    TenantDb tenantDb = getTenantDbByOrganizationId(tenantDto.getId(), organizationDb.getId());
    TenantDb oriTenantDb = copyDtoToEntity(tenantDto, tenantDb);

    // Create operation
    Long tenant = organizationDb.getTenant();
    OperationDb operation = updateOperation(tenant, userIdentifier, applicationId);
    operation.setProperty01(tenant.toString());

    // Add LifeCycle
    JsonNode tenantNode = JsonService.toJson(toDto(tenantDb));
    JsonNode oriTenantNode = JsonService.toJson(toDto(oriTenantDb));
    JsonNode patchNode = JsonDiff.asJson(tenantNode, oriTenantNode);
    String patch = JsonService.toString(patchNode);
    tenantDb.addLifeCycle(
        new LifeCycle(
            tenantDb.getAutoVersion(),
            operation.getId(),
            operation.getType(),
            operation.getCreated(),
            patch));
    tenantDb.incAutoVersion();
    tenantDb.setLastUpdate(operation.getCreated().toLocalDate());
    return toDto(tenantRepository.save(tenantDb));
  }

  private TenantDb copyDtoToEntity(TenantDto tenantDto, TenantDb tenantDb) {
    // Keep off non-updatable fields
    TenantDb oriTenantDb = Utils.copyProperties(tenantDb, new TenantDb());
    Utils.copyProperties(tenantDto, tenantDb);
    tenantDb.setCreationDate(oriTenantDb.getCreationDate());
    tenantDb.setLastUpdate(oriTenantDb.getLastUpdate());
    tenantDb.setOperationId(oriTenantDb.getOperationId());
    tenantDb.setAutoVersion(oriTenantDb.getAutoVersion());
    tenantDb.setLfcs(oriTenantDb.getLfcs());
    tenantDb.setEncrypted(oriTenantDb.getEncrypted());
    tenantDb.setStorageOffers(oriTenantDb.getStorageOffers());
    return oriTenantDb;
  }

  private OperationDb createOperation(Long tenant, String userIdentifier, String applicationId) {
    OperationDb op = OperationFactory.createTenantOp(tenant, userIdentifier, applicationId);
    op.setStatus(OperationStatus.BACKUP);
    op.setMessage("Backuping tenants");
    return operationService.save(op);
  }

  private OperationDb updateOperation(Long tenant, String userIdentifier, String applicationId) {
    OperationDb op = OperationFactory.updateTenantOp(tenant, userIdentifier, applicationId);
    op.setStatus(OperationStatus.BACKUP);
    op.setMessage("Backuping tenants");
    return operationService.save(op);
  }

  private void checkCreateTenant(TenantDto dto) {
    if (dto.getId() != null) {
      throw new BadRequestException(
          FAILED_TO_CREATE_TENANT, String.format("Non null identifier '%s'", dto.getId()));
    }
    checkUpdateTenant(dto);
  }

  private void checkUpdateTenant(TenantDto dto) {
    Set<String> s = new HashSet<>(dto.getStorageOffers());
    if (s.size() != dto.getStorageOffers().size()) {
      throw new BadRequestException(FAILED_TO_CREATE_TENANT, "Duplicate offers");
    }

    for (String storage : dto.getStorageOffers()) {
      if (!storageService.existsStorageOffer(storage)) {
        throw new BadRequestException(
            FAILED_TO_CREATE_TENANT, String.format("Unknown storage offer '%s'", storage));
      }
    }
  }

  @Transactional
  public TenantDto getTenant(Long organizationId, Long tenant) {
    Assert.notNull(organizationId, "organizationId must be not null");
    Assert.notNull(tenant, "tenant must be not null");

    return tenantRepository
        .findByIdAndOrganizationId(tenant, organizationId)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new NotFoundException(
                    TENANT_NOT_FOUND, String.format(NOT_FOUND, ENTITY_NAME, tenant)));
  }

  @Transactional
  public List<TenantDb> getTenantDbs(Long organizationId) {
    return tenantRepository.getByOrganizationId(organizationId).stream().toList();
  }

  @Transactional
  public List<TenantDto> getTenantDtos(Long organizationId) {
    return tenantRepository.getByOrganizationId(organizationId).stream().map(this::toDto).toList();
  }

  // Internal use
  public TenantDb getTenantDb(Long tenant) {
    return tenantRepository
        .findById(tenant)
        .orElseThrow(
            () ->
                new NotFoundException(
                    TENANT_NOT_FOUND, String.format(NOT_FOUND, ENTITY_NAME, tenant)));
  }

  public TenantDb getTenantDbByOrganizationId(Long tenant, Long organizationId) {
    return tenantRepository
        .findByIdAndOrganizationId(tenant, organizationId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    TENANT_NOT_FOUND, String.format(NOT_FOUND, ENTITY_NAME, tenant)));
  }

  public TenantDb getTenantDbForUpdate(Long tenant) {
    return tenantRepository
        .findForUpdate(tenant)
        .orElseThrow(
            () ->
                new NotFoundException(
                    TENANT_NOT_FOUND, String.format(NOT_FOUND, ENTITY_NAME, tenant)));
  }

  public boolean existsTenantByIdAndOrganizationId(Long tenant, Long organizationId) {
    return tenantRepository.existsByIdAndOrganizationId(tenant, organizationId);
  }

  public List<TenantDb> getTenantsDb() {
    return tenantRepository.findAll();
  }
}
