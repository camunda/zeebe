/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.bpm.platform.servlet;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.camunda.bpm.engine.ProcessEngineServices;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.impl.ManagementServiceImpl;
import org.camunda.bpm.engine.impl.management.PurgeReport;

@WebServlet(
    name = "PurgeEngineServlet",
    urlPatterns = {"/purge"})
public class PurgeServlet extends HttpServlet {
  private ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {
    final String engineName = req.getParameter("name");

    final List<ManagementServiceImpl> managementServices =
        ProcessEngines.getProcessEngines().values().stream()
            // if name is provided only purge that engine, otherwise all >:D
            .filter(
                processEngine ->
                    Optional.ofNullable(engineName)
                        .map(name -> processEngine.getName().equals(name))
                        .orElse(true))
            .map(ProcessEngineServices::getManagementService)
            .map(managementService -> (ManagementServiceImpl) managementService)
            .collect(toList());

    for (ManagementServiceImpl managementService : managementServices) {
      PurgeReport purgeReport = managementService.purge();
      resp.setCharacterEncoding("UTF-8");
      resp.getWriter().println(objectMapper.writeValueAsString(purgeReport));
    }
    resp.getWriter().flush();
    resp.setStatus(200);
    resp.setContentType("application/json");
  }
}
