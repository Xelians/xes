/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.bucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class DoubleBucket extends Bucket {

  @JsonProperty("key")
  private final double key;

  public DoubleBucket(@JsonProperty("docCount") long docCount, @JsonProperty("key") double key) {
    super(docCount);
    this.key = key;
  }
}
