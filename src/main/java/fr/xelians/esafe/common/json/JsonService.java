/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.xelians.esafe.archive.domain.atr.ArchiveTransferReply;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.referential.entity.AgencyDb;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class JsonService {

  private static final ObjectMapper writeMapper;
  private static final ObjectMapper readMapper;
  private static final ObjectWriter objectWriter;
  private static final ObjectReader objectReader;
  private static final ObjectWriter indentWriter;

  static {
    writeMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    writeMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    writeMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    writeMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

    objectWriter = writeMapper.writer();
    indentWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);

    // The read mapper must fail on unknown properties
    readMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    readMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    readMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    objectReader = readMapper.reader();
  }

  private JsonService() {}

  public static JsonNode toJson(Object object) {
    Validate.notNull(object, Utils.NOT_NULL, "object");
    return writeMapper.valueToTree(object);
  }

  public static String toString(Object object) {
    Validate.notNull(object, Utils.NOT_NULL, "object");
    try {
      return objectWriter.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      // This should never happen
      throw new InternalException("Failed to convert object to json string", "", e);
    }
  }

  public static byte[] toBytes(Object object, JsonConfig config) {
    Validate.notNull(object, Utils.NOT_NULL, "object");
    Validate.notNull(config, Utils.NOT_NULL, "config");

    ObjectWriter writer = config.format() ? indentWriter : objectWriter;
    try {
      return writer.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      // This should never happen
      throw new InternalException("Failed to convert object to json bytes", "", e);
    }
  }

  public static <C extends Collection<?>> byte[] collToBytes(C objects, JsonConfig config) {
    Validate.notNull(objects, Utils.NOT_NULL, "objects");
    Validate.notNull(config, Utils.NOT_NULL, "config");

    ObjectWriter writer = config.format() ? indentWriter : objectWriter;
    try (ByteArrayOutputStream os = new ByteArrayOutputStream(2048)) {
      writer.writeValues(os).writeAll(objects);
      return os.toByteArray();
    } catch (IOException e) {
      // This should never happen because we use a ByteArrayOutputStream
      throw new InternalException("Failed to convert list of objects to json bytes", "", e);
    }
  }

  public static List<LifeCycle> toLifeCycles(byte[] bytes) throws IOException {
    return toList(bytes, LifeCycle.class);
  }

  public static ArchiveUnit toArchiveUnit(JsonNode node) throws JsonProcessingException {
    return to(node, ArchiveUnit.class);
  }

  public static List<ArchiveUnit> toArchiveUnits(byte[] bytes) throws IOException {
    return toList(bytes, ArchiveUnit.class);
  }

  public static List<AgencyDb> toAgencies(byte[] bytes) throws IOException {
    return toList(bytes, AgencyDb.class);
  }

  // Caller must close the inputstream
  public static List<ArchiveUnit> toArchiveUnits(InputStream inputStream) throws IOException {
    return toList(inputStream, ArchiveUnit.class);
  }

  public static ArchiveTransferReply toArchiveTransferReply(InputStream inputStream)
      throws IOException {
    return to(inputStream, ArchiveTransferReply.class);
  }

  public static ArchiveTransferReply toArchiveTransferReply(byte[] bytes) throws IOException {
    return to(bytes, ArchiveTransferReply.class);
  }

  public static ArchiveTransferReply toArchiveTransferReply(Path path) throws IOException {
    return to(path, ArchiveTransferReply.class);
  }

  // Caller must close the inputstream
  public static Iterator<ArchiveUnit> toArchiveUnitIterator(InputStream inputStream)
      throws IOException {
    return toIterator(inputStream, ArchiveUnit.class);
  }

  public static UpdateQuery toUpdateQuery(Path path) throws IOException {
    return to(path, UpdateQuery.class);
  }

  public static void write(Object value, Path path, JsonConfig config) throws IOException {
    Validate.notNull(value, Utils.NOT_NULL, "value");
    Validate.notNull(path, Utils.NOT_NULL, "path");
    Validate.notNull(config, Utils.NOT_NULL, "config");

    ObjectWriter writer = config.format() ? indentWriter : objectWriter;
    try (OutputStream os = Files.newOutputStream(path)) {
      writer.writeValue(os, value);
    } catch (IOException ex) {
      throw new IOException(String.format("Failed to write json to '%s'", path), ex);
    }
  }

  // Caller must close the outputstream
  public static void write(Object value, JsonGenerator generator, JsonConfig config)
      throws IOException {
    Validate.notNull(value, Utils.NOT_NULL, "value");
    Validate.notNull(generator, Utils.NOT_NULL, "generator");
    Validate.notNull(config, Utils.NOT_NULL, "config");

    ObjectWriter writer = config.format() ? indentWriter : objectWriter;

    writer.writeValue(generator, value);
  }

  public static <C extends Collection<?>> void writeColl(C values, Path path, JsonConfig config)
      throws IOException {
    Validate.notNull(values, Utils.NOT_NULL, "values");
    Validate.notNull(path, Utils.NOT_NULL, "path");
    Validate.notNull(config, Utils.NOT_NULL, "config");

    ObjectWriter writer = config.format() ? indentWriter : objectWriter;
    try (OutputStream os = Files.newOutputStream(path)) {
      writer.writeValues(os).writeAll(values);
    } catch (IOException ex) {
      throw new IOException(String.format("Failed to write json list to '%s'", path), ex);
    }
  }

  // The sequence writer must be closed by the caller
  public static SequenceWriter createSequenceWriter(Path path) throws IOException {
    return createSequenceWriter(path, JsonConfig.DEFAULT);
  }

  public static SequenceWriter createSequenceWriter(Path path, JsonConfig config)
      throws IOException {
    Validate.notNull(path, Utils.NOT_NULL, "path");
    Validate.notNull(config, Utils.NOT_NULL, "config");

    ObjectWriter writer = config.format() ? indentWriter : objectWriter;
    try {
      OutputStream os = Files.newOutputStream(path);
      return writer.writeValues(os);
    } catch (IOException ex) {
      throw new IOException(String.format("Failed to write json list to '%s'", path), ex);
    }
  }

  // The generator must be closed by the caller
  public static JsonGenerator createGenerator(OutputStream os) throws IOException {
    return createGenerator(os, JsonConfig.DEFAULT);
  }

  public static JsonGenerator createGenerator(OutputStream os, JsonConfig config)
      throws IOException {
    ObjectWriter writer = config.format() ? indentWriter : objectWriter;
    return writer.createGenerator(os);
  }

  public static <T> T to(JsonNode node, Class<T> klass) throws JsonProcessingException {
    Validate.notNull(node, Utils.NOT_NULL, "node");
    return objectReader.treeToValue(node, klass);
  }

  // Caller must close the inputstream
  public static <T> T to(InputStream inputStream, Class<T> klass) throws IOException {
    Validate.notNull(inputStream, Utils.NOT_NULL, "inputStream");
    return objectReader.readValue(inputStream, klass);
  }

  public static <T> T to(byte[] bytes, Class<T> klass) throws IOException {
    Validate.notNull(bytes, Utils.NOT_NULL, "inputStream");
    return objectReader.readValue(bytes, klass);
  }

  public static <T> T to(Path path, Class<T> klass) throws IOException {
    Validate.notNull(path, Utils.NOT_NULL, "path");

    try (InputStream is = Files.newInputStream(path)) {
      return objectReader.readValue(is, klass);
    } catch (IOException ex) {
      throw new IOException(
          String.format("Failed to create '%s' from path '%s'", klass.getSimpleName(), path), ex);
    }
  }

  public static <T> T to(String str, Class<T> klass) throws IOException {
    Validate.notNull(str, Utils.NOT_NULL, "str");
    return objectReader.readValue(str, klass);
  }

  private static <T> List<T> toList(byte[] bytes, Class<T> klass) throws IOException {
    Validate.notNull(bytes, Utils.NOT_NULL, "bytes");

    List<T> list = new ArrayList<>();
    try (JsonParser jsonParser = objectReader.getFactory().createParser(bytes)) {
      for (Iterator<T> it = objectReader.readValues(jsonParser, klass); it.hasNext(); ) {
        list.add(it.next());
      }
      return list;
    }
  }

  // Caller must close the inputstream
  private static <T> List<T> toList(InputStream inputStream, Class<T> klass) throws IOException {
    Validate.notNull(inputStream, Utils.NOT_NULL, "inputStream");

    List<T> list = new ArrayList<>();
    JsonParser jsonParser = objectReader.getFactory().createParser(inputStream);
    for (Iterator<T> it = objectReader.readValues(jsonParser, klass); it.hasNext(); ) {
      list.add(it.next());
    }
    return list;
  }

  // Caller must close the inputstream
  private static <T> Iterator<T> toIterator(InputStream inputStream, Class<T> klass)
      throws IOException {
    Validate.notNull(inputStream, Utils.NOT_NULL, "inputStream");

    JsonParser jsonParser = objectReader.getFactory().createParser(inputStream);
    return objectReader.readValues(jsonParser, klass);
  }

  public static <T> List<T> toList(Path path, Class<T> klass) throws IOException {
    Validate.notNull(path, Utils.NOT_NULL, "path");

    try (InputStream is = Files.newInputStream(path)) {
      return toList(is, klass);
    } catch (IOException ex) {
      throw new IOException(
          String.format("Failed to create list '%s' from path '%s'", klass.getSimpleName(), path),
          ex);
    }
  }
}
