/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record UpdateRules(
    @JsonProperty("Rules") List<UpdateRule> rules,
    @JsonProperty("PreventInheritance") Boolean preventInheritance,
    @JsonProperty("FinalAction") String finalAction) {}
