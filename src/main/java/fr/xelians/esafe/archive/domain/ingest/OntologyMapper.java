/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest;

import fr.xelians.esafe.archive.domain.ingest.sedav2.ontology.XamOntology;
import fr.xelians.esafe.referential.service.OntologyService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;

// This class is thread safe however one ontologyMapper instance must not be shared between multiple
// tenants
public class OntologyMapper {

  // Cache ontology for DocumentType for the given tenant
  private final Map<String, OntologyMap> ontologyCache = new ConcurrentHashMap<>();
  private final OntologyService ontologyService;
  private final Long tenant;

  public OntologyMapper(OntologyService ontologyService, Long tenant) {
    this.ontologyService = ontologyService;
    this.tenant = tenant;
  }

  // Get the map for the specified document type (value of the documentType key).
  // The document type is case-sensitive.
  public OntologyMap getOntologyMap(String documentType) {
    if (StringUtils.isBlank(documentType)) {
      return XamOntology.MAPPING;
    }

    OntologyMap ontologyMap = ontologyCache.get(documentType);
    if (ontologyMap == null) {
      var mappings = ontologyService.getMappings(tenant, documentType);
      ontologyMap = mappings == null ? XamOntology.MAPPING : new BasicOntologyMap(mappings);
      ontologyCache.put(documentType, ontologyMap);
    }
    return ontologyMap;
  }
}
