/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.atr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/*
 * @author Emmanuel Deviller
 */
public record Reply(
    @NotNull @JsonProperty("MessageIdentifier") String messageIdentifier,
    @NotNull @JsonProperty("ReplyCode") String replyCode) {}
