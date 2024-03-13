/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.bucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonDeserialize(using = BucketDeserializer.class)
public class Bucket {

  @JsonProperty("docCount")
  private final long docCount;

  public Bucket(long docCount) {
    this.docCount = docCount;
  }
}
