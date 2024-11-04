/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.servlet;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/*
 * @author Emmanuel Deviller
 */
public class LimitedHttpServletRequest extends HttpServletRequestWrapper {
  private final long maxLength;

  public LimitedHttpServletRequest(HttpServletRequest request, long maxLength) {
    super(request);
    this.maxLength = maxLength;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new LimitedServletInputStream(super.getInputStream(), maxLength);
  }
}
