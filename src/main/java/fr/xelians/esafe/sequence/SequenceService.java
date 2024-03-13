/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
