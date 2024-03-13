/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterDto {

  @JsonProperty("$limit")
  private Integer limit;

  @JsonProperty("$orderby")
  private OrderbyDto orderBy;

  @Getter
  @Setter
  @ToString
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OrderbyDto {

    @JsonProperty("TransactedDate")
    private Integer transactedDate;
  }
}
