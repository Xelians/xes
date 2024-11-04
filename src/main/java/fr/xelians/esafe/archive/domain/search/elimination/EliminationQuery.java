/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.elimination;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/*
 * @author Emmanuel Deviller
 */
public record EliminationQuery(
    @NotNull @JsonProperty("dslRequest") SearchQuery searchQuery,
    @NotNull @JsonProperty("date") LocalDate eliminationDate) {}
