/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.testcommon;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.ResponseEntity;

@Slf4j
public class TestUtils {

  public static String pad(int n) {
    return StringUtils.leftPad(String.valueOf(n), 6, '0');
  }

  public static String getBody(ResponseEntity<?> response) {
    return response.getBody() == null ? "Response body is empty" : response.getBody().toString();
  }

  public static List<Path> filenamesStartWith(Path dir, String prefix, String suffix)
      throws IOException {
    try (Stream<Path> paths = Files.list(dir)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().startsWith(prefix))
          .filter(p -> p.getFileName().toString().endsWith(suffix))
          .sorted()
          .toList();
    }
  }

  public static List<Path> filenamesContain(Path dir, String pattern, String suffix)
      throws IOException {
    try (Stream<Path> paths = Files.list(dir)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().contains(pattern))
          .filter(p -> p.getFileName().toString().endsWith(suffix))
          .sorted()
          .toList();
    }
  }

  public static List<Path> filenamesContainDeep(Path dir, String pattern, String suffix)
      throws IOException {
    try (Stream<Path> paths = Files.walk(dir)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().contains(pattern))
          .filter(p -> p.getFileName().toString().endsWith(suffix))
          .sorted()
          .toList();
    }
  }

  public static void createPdf(String message, Path path) throws IOException {
    try (OutputStream os = Files.newOutputStream(path);
        BufferedOutputStream bos = new BufferedOutputStream(os)) {
      createPdf(message, bos);
    }
  }

  public static void createPdf(String message, OutputStream os) throws IOException {

    PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage();
      doc.addPage(page);
      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        cs.beginText();
        cs.setFont(font, 24);
        cs.newLineAtOffset(100, 700);
        cs.showText(message);
        cs.endText();
      }
      doc.save(os);
    }
  }
}
