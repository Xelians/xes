/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VitamLogbookOperationDto extends LogbookEventDto {

  public static final String EVENT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

  @JsonProperty("#id")
  @JsonAlias({"_id"})
  private String id;

  @JsonProperty("#tenant")
  @JsonAlias({"_tenant"})
  private Long tenant;

  private String agIdApp;
  private String evIdAppSession;
  private String evIdReq;
  private String agIdExt;
  private String obIdReq;
  private String obIdIn;
  private List<EventDto> events;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class EventDto extends LogbookEventDto {
    private String evIdReq;
    private String agIdExt;
    private String obIdReq;
    private String obIdIn;
  }
}
