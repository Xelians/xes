/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.common.constant.Env;
import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class NioUtils {

  private NioUtils() {}

  public long concat(File srcFile, File dstFile) throws IOException {
    try (FileInputStream fis = new FileInputStream(srcFile);
        FileOutputStream fos = new FileOutputStream(dstFile, true)) {

      FileChannel srcChannel = fis.getChannel();
      FileChannel dstChannel = fos.getChannel();
      return srcChannel.transferTo(0, srcChannel.size(), dstChannel);
    }
  }

  /**
   * Ajoute le contenu du path source au path cible.
   *
   * @param srcPath le path cible
   */
  public static long concat(Path srcPath, Path trgPath) throws IOException {
    try (InputStream is = Files.newInputStream(srcPath);
        OutputStream os = Files.newOutputStream(trgPath, StandardOpenOption.APPEND)) {
      return IOUtils.copy(is, os);
    }
  }

  /**
   * Crée une URI à partir du path.
   *
   * @param zipPath le path du zip
   * @return l 'URI
   */
  public static URI createZipURI(Path zipPath) {
    Validate.notNull(zipPath, Utils.NOT_NULL, "zipPath");

    try {
      return new URI("jar:file", zipPath.toAbsolutePath().toUri().getPath(), null);
    } catch (URISyntaxException ex) {
      throw new InternalException(
          "Zip URI creation failed", String.format("Unable to create zip URI for %s", zipPath), ex);
    }
  }

  /**
   * Crée un système de fichier de type Zip à partir du path.
   *
   * @param zipPath le path du zip
   * @return le système de fichier de type Zip
   */
  public static FileSystem newZipFileSystem(Path zipPath) {
    Validate.notNull(zipPath, Utils.NOT_NULL, "zipPath");

    URI zipURI = createZipURI(zipPath);
    Map<String, String> zipMap = Map.of("create", "true");

    try {
      return FileSystems.newFileSystem(zipURI, zipMap);
    } catch (IOException ex) {
      throw new InternalException(
          "Zip FS creation failed",
          String.format("Unable to create zip file system for %s", zipPath),
          ex);
    }
  }

  /**
   * Delete path quietly.
   *
   * @param path the path
   */
  public static void deletePathQuietly(Path path) {
    if (path != null) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        // Ignore
      }
    }
  }

  /**
   * Delete dir quietly.
   *
   * @param path the path
   */
  public static void deleteDirQuietly(Path path) {
    if (path != null && Files.exists(path)) {
      try (Stream<Path> walk = Files.walk(path)) {
        walk.sorted(Comparator.reverseOrder()).forEach(NioUtils::deletePathQuietly);
      } catch (IOException e) {
        // Ignore
      }
    }
  }

  public static String readFirstLine(Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      return reader.readLine();
    }
  }

  public static void closeQuietly(List<? extends Closeable> closables) {
    if (closables != null) {
      closables.forEach(IOUtils::closeQuietly);
    }
  }

  public static Path createTempFile() throws IOException {
    return Files.createTempFile(Env.TMP_PATH, null, null);
  }
}
