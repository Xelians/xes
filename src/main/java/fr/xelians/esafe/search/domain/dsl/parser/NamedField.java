/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.parser;

import fr.xelians.esafe.search.domain.field.Field;

/*
 * @author Emmanuel Deviller
 */
public record NamedField(String fieldName, Field field) {}
