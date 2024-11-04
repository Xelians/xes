/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.antivirus;

/*
 * @author Emmanuel Deviller
 */
public record ScanResult(ScanStatus status, String detail) {
  public static final ScanResult OK = new ScanResult(ScanStatus.OK, null);
}
