/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.export;

/*
 * @author Emmanuel Deviller
 */
public record RequestParameters(
    String comment,
    String archivalAgreement,
    String archivalAgencyIdentifier,
    String originatingAgencyIdentifier,
    String submissionAgencyIdentifier,
    String messageRequestIdentifier,
    String authorizationRequestReplyIdentifier,
    String transferringAgency,
    String requester) {}
