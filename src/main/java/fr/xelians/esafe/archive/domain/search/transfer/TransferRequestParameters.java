/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.export.RequestParameters;
import jakarta.validation.constraints.Size;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record TransferRequestParameters(
    @JsonProperty("comment") @Size(max = 10_000) String comment,
    @JsonProperty("archivalAgreement") @Size(max = 1024) String archivalAgreement,
    @JsonProperty("archivalAgencyIdentifier") @Size(max = 1024) String archivalAgencyIdentifier,
    @JsonProperty("originatingAgencyIdentifier") @Size(max = 1024)
        String originatingAgencyIdentifier,
    @JsonProperty("submissionAgencyIdentifier") @Size(max = 1024) String submissionAgencyIdentifier,
    @JsonProperty("transferringAgency") @Size(max = 1024) String transferringAgency,
    @JsonProperty("transferRequestReplyIdentifier") @Size(max = 1024)
        String transferRequestReplyIdentifier,
    @JsonProperty("relatedTransferReference") @Size(max = 1024)
        List<String> relatedTransferReference) {

  public RequestParameters toRequestParameters() {
    return new RequestParameters(
        comment,
        archivalAgreement,
        archivalAgencyIdentifier,
        originatingAgencyIdentifier,
        submissionAgencyIdentifier != null ? submissionAgencyIdentifier : archivalAgencyIdentifier,
        transferRequestReplyIdentifier,
        null,
        transferringAgency != null ? transferringAgency : archivalAgencyIdentifier,
        null);
  }
}
