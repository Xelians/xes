/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.parser;

import fr.xelians.esafe.search.domain.field.Field;

public record NamedField(String fieldName, Field field) {}
