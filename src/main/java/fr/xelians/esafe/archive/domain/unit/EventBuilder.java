/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class EventBuilder {

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

  private EventBuilder() {}

  public static EventBuilder builder() {
    return new EventBuilder();
  }

  public EventBuilder withIdentifier(@JsonProperty("Identifier") String identifier) {
    this.identifier = identifier;
    return this;
  }

  public EventBuilder withTypeCode(@JsonProperty("TypeCode") String typeCode) {
    this.typeCode = typeCode;
    return this;
  }

  /**
   * Spécifie le type de l'événement.
   *
   * @param type le type
   * @return le builder
   */
  public EventBuilder withType(@JsonProperty("Type") String type) {
    this.type = type;
    return this;
  }

  /**
   * Spécifie la date et l'heure.
   *
   * @param dateTime la date et l'heure
   * @return le builder
   */
  public EventBuilder withDateTime(@JsonProperty("DateTime") LocalDateTime dateTime) {
    this.dateTime = dateTime;
    return this;
  }

  /**
   * Spécifie le détail de l'événement.
   *
   * @param detail le détail
   * @return le builder
   */
  public EventBuilder withDetail(@JsonProperty("Detail") String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Spécifie le résultat de l'événement.
   *
   * @param outcome le résultat
   * @return le builder
   */
  public EventBuilder withOutcome(@JsonProperty("Outcome") String outcome) {
    this.outcome = outcome;
    return this;
  }

  /**
   * Spécifie le détail du résultat de l'événement.
   *
   * @param outcomeDetail le détail du résultat
   * @return le builder
   */
  public EventBuilder withOutcomeDetail(@JsonProperty("OutcomeDetail") String outcomeDetail) {
    this.outcomeDetail = outcomeDetail;
    return this;
  }

  /**
   * Spécifie le message détail du résultat de l'événement.
   *
   * @param outcomeDetailMessage le message détail du résultat
   * @return le builder
   */
  public EventBuilder withOutcomeDetailMessage(
      @JsonProperty("OutcomeDetailMessage") String outcomeDetailMessage) {
    this.outcomeDetailMessage = outcomeDetailMessage;
    return this;
  }

  /**
   * Spécifie le message technique détaillant l'erreur de l'événement.
   *
   * @param detailData le message technique de l'erreur
   * @return le builder
   */
  public EventBuilder withDetailData(@JsonProperty("DetailData") String detailData) {
    this.detailData = detailData;
    return this;
  }

  /**
   * Instancie la classe Event selon les paramètres précédemment spécifiés dans le builder.
   *
   * @return l 'évènement
   */
  public Event build() {
    return new Event(
        identifier,
        typeCode,
        type,
        dateTime,
        detail,
        outcome,
        outcomeDetail,
        outcomeDetailMessage,
        detailData);
  }
}
