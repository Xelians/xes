/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

/*
 * @author Emmanuel Deviller
 */
public enum BinaryQualifier {
  BinaryMaster,
  Dissemination,
  Thumbnail,
  TextContent;

  public static boolean isValid(String str) {
    return BinaryMaster.toString().equals(str)
        || Dissemination.toString().equals(str)
        || Thumbnail.toString().equals(str)
        || TextContent.toString().equals(str);
  }
}
