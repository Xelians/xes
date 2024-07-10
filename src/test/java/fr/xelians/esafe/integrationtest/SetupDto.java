/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.UserDto;

public record SetupDto(Long tenant, SignupDto signupDto, UserDto userDto) {}
