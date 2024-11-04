/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
public class StorageInputStream extends FilterInputStream {

  public static final long UNKNOWN_LENGTH = -1L;

  private final long length;
  private final Path tmpPath;

  // This is the client responsibility to close this stream
  public StorageInputStream(long length, InputStream inputStream) {
    this(length, inputStream, null);
  }

  // This is the client responsibility to close this stream
  // The path represents a temporary file used to create the input stream
  public StorageInputStream(long length, InputStream inputStream, Path tmpPath) {
    super(inputStream);
    this.length = length;
    this.tmpPath = tmpPath;
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      // if the object was a temporary file then we have to delete it
      if (tmpPath != null) {
        Files.deleteIfExists(tmpPath);
      }
    }
  }
}
