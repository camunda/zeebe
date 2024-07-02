/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.jetty;

import static io.camunda.optimize.jetty.OptimizeResourceConstants.STATIC_RESOURCE_PATH;
import static io.camunda.optimize.jetty.OptimizeResourceConstants.STATUS_WEBSOCKET_PATH;
import static io.camunda.optimize.service.util.configuration.ConfigurationServiceConstants.CCSM_PROFILE;
import static io.camunda.optimize.service.util.configuration.ConfigurationServiceConstants.CLOUD_PROFILE;

import io.camunda.optimize.service.exceptions.license.OptimizeLicenseException;
import io.camunda.optimize.service.license.LicenseManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

@RequiredArgsConstructor
@Slf4j
public class LicenseFilter implements Filter {
  private final LicenseManager licenseManager;
  private final ApplicationContext applicationContext;
  private static final Set<String> EXCLUDED_EXTENSIONS = Set.of("css", "html", "js", "ico");
  private static final Set<String> EXCLUDED_API_CALLS =
      Set.of(
          "authentication",
          "localization",
          "ui-configuration",
          "license/validate-and-store",
          "license/validate",
          "status",
          "readyz");

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing to do here
  }

  /**
   * Before the user can access the Optimize APIs a license check is performed. Whenever there is an
   * invalid or no license, backend returns status code 403.
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletResponse servletResponse = (HttpServletResponse) response;
    HttpServletRequest servletRequest = (HttpServletRequest) request;

    if (isLicenseCheckNeeded(servletRequest, applicationContext)) {
      try {
        licenseManager.validateLicenseStoredInOptimize();
      } catch (OptimizeLicenseException e) {
        log.warn("Given License is invalid or not available!");
        constructForbiddenResponse(servletResponse, e);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private void constructForbiddenResponse(
      HttpServletResponse servletResponse, OptimizeLicenseException ex) throws IOException {
    servletResponse.getWriter().write("{\"errorCode\": \"" + ex.getErrorCode() + "\"}");
    servletResponse.setContentType("application/json");
    servletResponse.setCharacterEncoding("UTF-8");
    servletResponse.setStatus(Response.Status.FORBIDDEN.getStatusCode());
  }

  private static boolean isLicenseCheckNeeded(
      HttpServletRequest servletRequest, ApplicationContext applicationContext) {
    String requestPath = servletRequest.getServletPath().toLowerCase(Locale.ENGLISH);
    String pathInfo = servletRequest.getPathInfo();

    return !isStaticResource(requestPath)
        && !isRootRequest(requestPath)
        && !isCloudEnvironment(applicationContext)
        && !isExcludedApiPath(pathInfo)
        && !isStatusRequest(requestPath);
  }

  private static boolean isCloudEnvironment(ApplicationContext applicationContext) {
    return Arrays.stream(applicationContext.getEnvironment().getActiveProfiles())
        .anyMatch(
            profile ->
                CLOUD_PROFILE.equalsIgnoreCase(profile) || CCSM_PROFILE.equalsIgnoreCase(profile));
  }

  private static boolean isStatusRequest(String requestPath) {
    return requestPath.equals(STATUS_WEBSOCKET_PATH);
  }

  private static boolean isExcludedApiPath(String pathInfo) {
    return pathInfo != null && EXCLUDED_API_CALLS.stream().anyMatch(pathInfo::contains);
  }

  private static boolean isRootRequest(String requestPath) {
    return requestPath.equals("/");
  }

  private static boolean isStaticResource(String requestPath) {
    return requestPath.contains("^" + STATIC_RESOURCE_PATH + "/.+")
        || EXCLUDED_EXTENSIONS.stream().anyMatch(ext -> requestPath.endsWith("." + ext));
  }

  @Override
  public void destroy() {
    // nothing to do here
  }
}
