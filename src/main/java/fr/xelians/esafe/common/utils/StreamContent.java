/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import java.io.InputStream;

/*
 * @author Emmanuel Deviller
 */
public record StreamContent(String name, String mimetype, InputStream inputStream) {}
