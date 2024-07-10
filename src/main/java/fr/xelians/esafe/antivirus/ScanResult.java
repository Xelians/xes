package fr.xelians.esafe.antivirus;

public record ScanResult(ScanStatus status, String detail) {
  public static final ScanResult OK = new ScanResult(ScanStatus.OK, null);
}
