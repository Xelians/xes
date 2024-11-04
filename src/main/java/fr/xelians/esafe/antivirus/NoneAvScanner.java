/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.antivirus;

import fr.xelians.esafe.common.exception.technical.InternalException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class NoneAvScanner implements AntiVirusScanner {

  public static final NoneAvScanner INSTANCE = new NoneAvScanner();

  private NoneAvScanner() {
    // Nothing to do
  }

  @Override
  public AntiVirus getName() {
    return AntiVirus.None;
  }

  @Override
  public ScanResult scan(Path path) {
    throw new InternalException("None antivirus scanner cannot scan files");
  }
}
