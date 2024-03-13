/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@JsonDeserialize(builder = EventBuilder.class)
public class Event {

  @JsonProperty("Identifier")
  private String identifier;

  @JsonProperty("TypeCode")
  private String typeCode;

  @JsonProperty("Type")
  private String type;

  @JsonProperty("DateTime")
  private LocalDateTime dateTime;

  @JsonProperty("Detail")
  private String detail;

  @JsonProperty("Outcome")
  private String outcome;

  @JsonProperty("OutcomeDetail")
  private String outcomeDetail;

  @JsonProperty("OutcomeDetailMessage")
  private String outcomeDetailMessage;

  @JsonProperty("DetailData")
  private String detailData;

  public Event(
      String identifier,
      String typeCode,
      String type,
      LocalDateTime dateTime,
      String detail,
      String outcome,
      String outcomeDetail,
      String outcomeDetailMessage,
      String detailData) {

    this.identifier = identifier;
    this.typeCode = typeCode;
    this.type = type;
    this.dateTime = dateTime;
    this.detail = detail;
    this.outcome = outcome;
    this.outcomeDetail = outcomeDetail;
    this.outcomeDetailMessage = outcomeDetailMessage;
    this.detailData = detailData;
  }
}
