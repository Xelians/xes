/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record DipRequestParameters(
    @JsonProperty("archivalAgreement") @Size(max = 1024) String archivalAgreement,
    @JsonProperty("originatingAgencyIdentifier") @Size(max = 1024)
        String originatingAgencyIdentifier,
    @JsonProperty("comment") @Size(max = 10_000) String comment,
    @JsonProperty("submissionAgencyIdentifier") @Size(max = 1024) String submissionAgencyIdentifier,
    @JsonProperty("messageRequestIdentifier") @Size(max = 1024) String messageRequestIdentifier,
    @JsonProperty("authorizationRequestReplyIdentifier") @Size(max = 1024)
        String authorizationRequestReplyIdentifier,
    @JsonProperty("archivalAgencyIdentifier") @Size(max = 1024) String archivalAgencyIdentifier,
    @JsonProperty("requesterIdentifier") @Size(max = 1024) String requesterIdentifier) {}
