/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.sequence;

// Sequence is thread-safe
public class Sequence {

  public static final int ALLOCATION_SIZE = 50;

  private final SequenceRepository sequenceRepository;
  private long nextValue = 0;
  private long maxValue = 0;

  public Sequence(SequenceRepository sequenceRepository) {
    this.sequenceRepository = sequenceRepository;
  }

  public synchronized long nextValue() {
    if (nextValue >= maxValue) {
      maxValue = sequenceRepository.getNextValue();
      nextValue = maxValue - ALLOCATION_SIZE;
    }
    return ++nextValue;
  }
}
