/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.dto;

import fr.xelians.esafe.common.dto.BaseDto;

public interface ReferentialDto extends BaseDto {

  String getIdentifier();

  void setIdentifier(String identifier);

  Long getTenant();

  void setTenant(Long tenant);
}
