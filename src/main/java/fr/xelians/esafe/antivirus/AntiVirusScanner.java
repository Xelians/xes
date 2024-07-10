package fr.xelians.esafe.antivirus;

import java.io.IOException;
import java.nio.file.Path;

public interface AntiVirusScanner {

  AntiVirus getName();

  ScanResult scan(Path path) throws IOException;
}
