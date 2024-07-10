/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.domain;

public enum CheckParentLinkStatus {
  AUTHORIZED, // L'attachement depuis le manifest est autorisé (sous les CheckParentLink si présent)
  REQUIRED, // L'attachement depuis le manifest est obligatoire (sous les CheckParentLink si
  // présent)
  UNAUTHORIZED // L'attachement depuis le manifest est interdit
}
