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
public final class DateRangeBucket extends Bucket {

  @JsonProperty("from")
  private final String from;

  @JsonProperty("to")
  private final String to;

  @JsonProperty("key")
  private final String key;

  public DateRangeBucket(
      @JsonProperty("docCount") long docCount,
      @JsonProperty("from") String from,
      @JsonProperty("to") String to,
      @JsonProperty("key") String key) {
    super(docCount);
    this.from = from;
    this.to = to;
    this.key = key;
  }
}
