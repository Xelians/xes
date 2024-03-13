/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.lang3.Validate;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public final class Validators {

  private Validators() {}

  public static Validator getRngValidator(byte[] data) throws IOException {
    Validate.notNull(data, SipUtils.NOT_NULL, "rngPath");
    try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
        Reader rngReader = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
      return getRngValidator(rngReader);
    }
  }

  public static Validator getRngValidator(Path rngPath) throws IOException {
    Validate.notNull(rngPath, SipUtils.NOT_NULL, "rngPath");

    try (BufferedReader rngReader = Files.newBufferedReader(rngPath, StandardCharsets.UTF_8)) {
      return getRngValidator(rngReader);
    }
  }

  public static Validator getRngValidator(Reader rngReader) {
    Validate.notNull(rngReader, SipUtils.NOT_NULL, "rngReader");
    return getRngSchema(new StreamSource(rngReader)).newValidator();
  }

  private static Schema getRngSchema(StreamSource source) {
    try {
      // Initialize RNG validator through JAXP. SchemaFactory is not tread safe , so we create a new
      // one for each RNG schema. XXE mitigation is not supported
      System.setProperty(
          SchemaFactory.class.getName() + ":" + XMLConstants.RELAXNG_NS_URI,
          "com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory");
      SchemaFactory rngSchemaFactory = SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI);
      rngSchemaFactory.setProperty(
          "http://relaxng.org/properties/datatype-library-factory",
          new org.relaxng.datatype.helpers.DatatypeLibraryLoader());
      return rngSchemaFactory.newSchema(source);
    } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
      throw new InternalException(
          "Rng schema creation failed", "Unable to initialize RNG Factory", ex);
    } catch (SAXException ex) {
      throw new InternalException(
          "Rng schema creation failed", "Unable to create RNG Validator", ex);
    }
  }

  public static void validate(Path path, Validator validator) throws IOException, SAXException {
    Validate.notNull(path, SipUtils.NOT_NULL, "source");
    Validate.notNull(validator, SipUtils.NOT_NULL, "validator");

    try (InputStream is = Files.newInputStream(path)) {
      validator.validate(new StreamSource(is));
    }
  }
}
