/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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
