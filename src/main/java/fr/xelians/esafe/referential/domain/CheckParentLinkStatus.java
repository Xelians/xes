/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.domain;

public enum CheckParentLinkStatus {
  AUTHORIZED, // L'attachement depuis le manifest est autorisé (sous les CheckParentLink si présent)
  REQUIRED, // L'attachement depuis le manifest est obligatoire (sous les CheckParentLink si
  // présent)
  UNAUTHORIZED // L'attachement depuis le manifest est interdit
}
