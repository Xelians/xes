/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.sequence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SequenceService {

  private final SequenceRepository sequenceRepository;

  public Sequence createSequence() {
    return new Sequence(sequenceRepository);
  }

  public long getNextValue() {
    return sequenceRepository.getNextValue();
  }

  public long getCurrentValue() {
    return sequenceRepository.getCurrentValue();
  }
}
