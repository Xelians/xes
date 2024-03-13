/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.domain.search;

import jakarta.persistence.TypedQuery;

public record Request<T>(TypedQuery<T> mainQuery, TypedQuery<Long> countQuery) {}
