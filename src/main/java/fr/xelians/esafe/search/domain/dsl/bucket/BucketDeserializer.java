/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.bucket;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.IOException;

public class BucketDeserializer extends StdDeserializer<Bucket> {

  public BucketDeserializer() {
    this(null);
  }

  public BucketDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Bucket deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    JsonNode node = jp.getCodec().readTree(jp);
    long docCount = node.get("docCount").asLong();

    JsonNode keyNode = node.get("key");
    if (keyNode == null) {
      return new Bucket(docCount);
    }

    if (keyNode.isTextual()) {
      JsonNode fromNode = node.get("from");
      JsonNode toNode = node.get("to");
      if (fromNode == null && toNode == null) {
        return new StringBucket(docCount, keyNode.asText());
      }
      String from = fromNode == null ? null : fromNode.asText();
      String to = toNode == null ? null : toNode.asText();
      return new DateRangeBucket(docCount, from, to, keyNode.asText());
    }

    if (keyNode.isLong() || keyNode.isInt()) {
      return new LongBucket(docCount, keyNode.asLong());
    }

    if (keyNode.isDouble() || keyNode.isFloat()) {
      return new DoubleBucket(docCount, keyNode.asDouble());
    }

    throw new InternalException(
        "Failed to deserialize Bucket", String.format("Unknown bucket '%s'", node));
  }
}
