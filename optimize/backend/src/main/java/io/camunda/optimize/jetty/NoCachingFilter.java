/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.jetty;

import static io.camunda.optimize.jetty.OptimizeResourceConstants.NO_CACHE_RESOURCES;
import static io.camunda.optimize.jetty.OptimizeResourceConstants.REST_API_PATH;
import static io.camunda.optimize.rest.constants.RestConstants.CACHE_CONTROL_NO_STORE;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * After an upgrade of Optimize the browser might load old resources from the browser cache. This
 * filter adds the no store cookie to the response to prevent those caching issues after the
 * upgrade.
 */
public class NoCachingFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing to do here
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    String requestPath = servletRequest.getServletPath();
    boolean isStaticResourceThatShouldNotBeCached =
        NO_CACHE_RESOURCES.stream().anyMatch(requestPath::endsWith);
    if (isStaticResourceThatShouldNotBeCached || isApiRestCall(requestPath)) {
      ((HttpServletResponse) response).setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE);
    }
    chain.doFilter(request, response);
  }

  private boolean isApiRestCall(String requestPath) {
    return requestPath.startsWith(REST_API_PATH);
  }

  @Override
  public void destroy() {
    // nothing to do here
  }
}
