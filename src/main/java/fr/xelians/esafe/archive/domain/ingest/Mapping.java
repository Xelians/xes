/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * @author Emmanuel Deviller
 */
public record Mapping(@JsonProperty("Src") String src, @JsonProperty("Dst") String dst) {}
