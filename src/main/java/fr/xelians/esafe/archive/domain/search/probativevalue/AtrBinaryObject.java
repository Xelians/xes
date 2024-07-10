/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.probativevalue;

import fr.xelians.esafe.archive.domain.atr.BinaryDataObjectReply;
import java.time.LocalDateTime;

public record AtrBinaryObject(LocalDateTime grantDate, BinaryDataObjectReply bdoReply) {}
