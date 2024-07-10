/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner;

import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import java.io.IOException;

public interface OperationProcessor {

  void process(LogbookOperation operation) throws IOException;

  void finish() throws IOException;
}
