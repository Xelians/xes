/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.SipUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.lang3.Validate;
import org.xml.sax.SAXException;

public class Sedav2Validator {

  private static final String HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1 =
      "http://www.w3.org/XML/XMLSchema/v1.1";

  private static final Sedav2Validator V21_INSTANCE =
      new Sedav2Validator("seda-vitam-2.1-full.xsd", "2.1");

  private static final Sedav2Validator V22_INSTANCE =
      new Sedav2Validator("seda-vitam-2.2-full.xsd", "2.2");

  private final String sedaVersion;
  private final Schema sedaSchema;

  private Sedav2Validator(String mainXsd, String sedaVersion) {

    this.sedaVersion = sedaVersion;

    SchemaFactory sf = SchemaFactory.newInstance(HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1);

    // We provide a flattened schema to avoid multiple xsd ressources
    try (InputStream is1 = SipUtils.resourceAsStream(mainXsd);
        InputStream is2 = SipUtils.resourceAsStream("xml.xsd");
        InputStream is3 = SipUtils.resourceAsStream("xlink.xsd")) {

      sf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Avoid XXE
      sf.setResourceResolver(new Sedav2Resolver(is2, is3));
      sedaSchema = sf.newSchema(new StreamSource(is1));

    } catch (IOException | SAXException ex) {
      throw new InternalException(
          "Schema creation failed ",
          "Failed to initialize XSD Schemas, JAXBContext and Marshaller",
          ex);
    }
  }

  /**
   * Retourne l'instance singleton de la classe Sedav2Service.
   *
   * @return l 'instance singleton
   */
  public static Sedav2Validator getV21Instance() {
    return V21_INSTANCE;
  }

  public static Sedav2Validator getV22Instance() {
    return V22_INSTANCE;
  }

  public String getSedaVersion() {
    return sedaVersion;
  }

  /**
   * Valide le fichier XML selon le schéma défini par le standard SEDA v2
   *
   * @param path le path du fichier XML à valider
   */
  public void validate(Path path) throws IOException, SAXException {
    Validate.notNull(path, SipUtils.NOT_NULL, "path");

    try (InputStream is = Files.newInputStream(path)) {
      Validator sedaValidator = sedaSchema.newValidator();
      sedaValidator.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      sedaValidator.validate(new StreamSource(is));
    }
  }

  /**
   * Valide le fichier XML avec le validateur indiqué. L'objet Validator n'est pas thread-safe, il
   * est de la responsabilité de l'application appelante de s'assurer que l'objet {@link Validator}
   * n'est utilisé à tout moment que par une seule et même thread.
   *
   * @param path le path du fichier XML à valider
   * @param validator le validateur RNG
   */
  public static void validate(Path path, Validator validator) throws IOException, SAXException {
    Validate.notNull(path, SipUtils.NOT_NULL, "path");
    Validate.notNull(validator, SipUtils.NOT_NULL, "validator");

    // Check manifest is valid against rng
    try (InputStream is = Files.newInputStream(path)) {
      validator.validate(new StreamSource(is));
    }
  }
}
