/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.UserDto;

public record SetupDto(Long tenant, SignupDto signupDto, UserDto userDto) {}
