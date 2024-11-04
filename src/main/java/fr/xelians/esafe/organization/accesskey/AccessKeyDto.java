/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.accesskey;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/*
 * @author Youcef Bouhaddouza
 */
@JsonNaming(SnakeCaseStrategy.class)
public record AccessKeyDto(String accessKey, String keyType, long expireIn) {}
