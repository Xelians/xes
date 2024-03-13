/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * La classe Sedav2Resolver permet de résoudre les accès aux schémas inclus dans les XSD utilisés
 * lors de la conversion en SEDA v2.1.
 *
 * @author Emmanuel Deviller
 */
public class Sedav2Resolver implements LSResourceResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(Sedav2Resolver.class);

  private final InputStream xmlInputStream;
  private final InputStream xlinkInputStream;

  /**
   * Instantiates a new Sedav 2 resolver.
   *
   * @param xmlInputStream the xml input stream
   * @param xlinkInputStream the xlink input stream
   */
  public Sedav2Resolver(InputStream xmlInputStream, InputStream xlinkInputStream) {
    this.xmlInputStream = xmlInputStream;
    this.xlinkInputStream = xlinkInputStream;
  }

  @Override
  public LSInput resolveResource(
      final String type,
      final String namespaceURI,
      final String publicId,
      String systemId,
      final String baseURI) {

    return switch (systemId) {
      case "http://www.w3.org/2001/xml.xsd" -> new LSInputImpl(publicId, systemId, xmlInputStream);
      case "http://www.w3.org/1999/xlink.xsd" -> new LSInputImpl(
          publicId, systemId, xlinkInputStream);
      default -> {
        LOGGER.info("Unable to resolve resource {}", systemId);
        yield null;
      }
    };
  }
}
