/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.update;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record UpdateResult<T>(
    String context, Integer from, Integer size, List<T> results, JsonNode jsonPatch) {}
