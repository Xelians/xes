package fr.xelians.esafe.logbook.service;

import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
import java.util.Collections;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LogbookMapper {

  LogbookOperationDto toLogbookOperationDto(LogbookOperation logbookOperation);

  @Mapping(target = "evId", source = "id")
  @Mapping(target = "evIdReq", ignore = true)
  @Mapping(target = "evParentId", ignore = true)
  @Mapping(target = "evType", source = "typeInfo")
  @Mapping(target = "evTypeProc", source = "logbookOperation", qualifiedByName = "buildTypeProc")
  @Mapping(target = "outMessg", source = "message")
  @Mapping(target = "outDetail", source = "logbookOperation", qualifiedByName = "buildOutDetail")
  @Mapping(target = "obId", source = "objectIdentifier")
  @Mapping(target = "obIdReq", source = "objectInfo")
  @Mapping(target = "obIdIn", ignore = true)
  @Mapping(target = "evIdProc", ignore = true)
  @Mapping(target = "evIdAppSession", source = "applicationId")
  @Mapping(target = "evDetData", source = "objectData")
  @Mapping(target = "agIdExt", ignore = true)
  @Mapping(target = "agIdApp", ignore = true)
  @Mapping(target = "agId", source = "userIdentifier")
  @Mapping(target = "events", ignore = true)
  @Mapping(target = "rightsStatementIdentifier", ignore = true)
  @Mapping(
      target = "evDateTime",
      source = "created",
      dateFormat = VitamLogbookOperationDto.EVENT_DATE_TIME_FORMAT)
  @Mapping(target = "lastPersistedDate", source = "modified")
  VitamLogbookOperationDto toVitamLogbookOperationDto(LogbookOperation logbookOperation);

  VitamLogbookOperationDto.EventDto toEvent(VitamLogbookOperationDto logbookOperationDto);

  @Named("buildOutDetail")
  default String buildOutDetail(LogbookOperation logbookOperation) {
    return logbookOperation.getTypeInfo() + "." + logbookOperation.getOutcome();
  }

  @Named("buildTypeProc")
  default String buildTypeProc(LogbookOperation logbookOperation) {
    return switch (logbookOperation.getType()) {
      case INGEST_HOLDING -> "MASTERDATA";
      case INGEST_ARCHIVE, INGEST_FILING -> "INGEST";
      case UPDATE_ARCHIVE, UPDATE_ARCHIVE_RULES -> "MASS_UPDATE";
      case TRANSFER_ARCHIVE -> "ARCHIVE_TRANSFER";
      case EXPORT_ARCHIVE -> "EXPORT_DIP";
      case RECLASSIFY_ARCHIVE -> "RECLASSIFICATION";
      case ELIMINATE_ARCHIVE -> "ELIMINATION";
      case PROBATIVE_VALUE -> "EXPORT_PROBATIVE_VALUE";
      case CHECK_COHERENCY -> "CHECK";
      default -> logbookOperation.getType().toString();
    };
  }

  @AfterMapping
  default void setEvents(LogbookOperation source, @MappingTarget VitamLogbookOperationDto target) {
    if (source != null) {
      target.setEvents(Collections.singletonList(toEvent(target)));
    }
  }
}
