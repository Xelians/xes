/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.pack;

import java.io.IOException;
import java.nio.file.Path;

public interface Packer extends AutoCloseable {

  long[] write(Path srcPath) throws IOException;
}
