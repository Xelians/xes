/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.dto;

import fr.xelians.esafe.common.dto.BaseDto;

public interface ReferentialDto extends BaseDto {

  String getIdentifier();

  void setIdentifier(String identifier);

  Long getTenant();

  void setTenant(Long tenant);
}
