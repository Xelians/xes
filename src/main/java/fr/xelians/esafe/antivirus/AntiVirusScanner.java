/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.antivirus;

import java.io.IOException;
import java.nio.file.Path;

/*
 * @author Emmanuel Deviller
 */
public interface AntiVirusScanner {

  AntiVirus getName();

  ScanResult scan(Path path) throws IOException;
}
