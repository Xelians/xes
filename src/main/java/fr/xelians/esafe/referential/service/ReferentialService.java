/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.service;

import fr.xelians.esafe.sequence.SequenceService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Getter
@Service
@RequiredArgsConstructor
public class ReferentialService {

  private final AgencyService agencyService;
  private final OntologyService ontologyService;
  private final RuleService ruleService;
  private final ProfileService profileService;
  private final IngestContractService ingestContractService;
  private final AccessContractService accessContractService;
  private final SequenceService sequenceService;
}
