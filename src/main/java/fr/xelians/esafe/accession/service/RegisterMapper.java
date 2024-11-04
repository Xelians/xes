/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.service;

import fr.xelians.esafe.accession.domain.model.Register;
import fr.xelians.esafe.accession.domain.model.RegisterDetails;
import fr.xelians.esafe.accession.dto.RegisterDetailsDto;
import fr.xelians.esafe.accession.dto.RegisterDto;
import org.mapstruct.Mapper;

/*
 * @author Emmanuel Deviller
 */
@Mapper(componentModel = "spring")
public interface RegisterMapper {

  RegisterDto toRegisterDto(Register register);

  RegisterDetailsDto toRegisterDetailsDto(RegisterDetails registerDetails);
}
