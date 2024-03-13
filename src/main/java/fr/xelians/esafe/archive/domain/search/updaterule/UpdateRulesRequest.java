/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import co.elastic.clients.elasticsearch.core.SearchRequest;

public record UpdateRulesRequest(SearchRequest searchRequest, RuleLists ruleLists) {}
