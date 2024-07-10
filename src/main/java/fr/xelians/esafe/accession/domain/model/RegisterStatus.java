package fr.xelians.esafe.accession.domain.model;

public enum RegisterStatus {
  /** indicates that the Accession register stored and completed */
  STORED_AND_COMPLETED("stored and completed"),

  /** indicates that the Accession register stored and updated */
  STORED_AND_UPDATED("stored and updated"),

  /** indicates that the Accession register is not stored */
  UNSTORED("unstored");

  private final String value;

  RegisterStatus(String val) {
    value = val;
  }

  /**
   * @return value
   */
  public String value() {
    return value;
  }
}
