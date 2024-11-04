/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.export.RequestParameters;
import jakarta.validation.constraints.Size;

/*
 * @author Emmanuel Deviller
 */
public record DipRequestParameters(
    @JsonProperty("comment") @Size(max = 10_000) String comment,
    @JsonProperty("archivalAgreement") @Size(max = 1024) String archivalAgreement,
    @JsonProperty("archivalAgencyIdentifier") @Size(max = 1024) String archivalAgencyIdentifier,
    @JsonProperty("originatingAgencyIdentifier") @Size(max = 1024)
        String originatingAgencyIdentifier,
    @JsonProperty("submissionAgencyIdentifier") @Size(max = 1024) String submissionAgencyIdentifier,
    @JsonProperty("requesterIdentifier") @Size(max = 1024) String requesterIdentifier,
    @JsonProperty("messageRequestIdentifier") @Size(max = 1024) String messageRequestIdentifier,
    @JsonProperty("authorizationRequestReplyIdentifier") @Size(max = 1024)
        String authorizationRequestReplyIdentifier) {

  public RequestParameters toRequestParameters() {
    return new RequestParameters(
        comment,
        archivalAgreement,
        archivalAgencyIdentifier,
        originatingAgencyIdentifier != null
            ? originatingAgencyIdentifier
            : archivalAgencyIdentifier,
        submissionAgencyIdentifier != null ? submissionAgencyIdentifier : archivalAgencyIdentifier,
        messageRequestIdentifier,
        authorizationRequestReplyIdentifier,
        archivalAgencyIdentifier,
        requesterIdentifier);
  }
}
