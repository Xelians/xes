package fr.xelians.esafe.accession.service;

import fr.xelians.esafe.accession.domain.model.RegisterDetails;
import fr.xelians.esafe.accession.domain.model.RegisterSummary;
import fr.xelians.esafe.accession.dto.RegisterDetailsDto;
import fr.xelians.esafe.accession.dto.RegisterSummaryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegisterMapper {

  RegisterSummaryDto toRegisterSummaryDto(RegisterSummary registerSummary);

  RegisterDetailsDto toRegisterDetailsDto(RegisterDetails registerDetails);
}
