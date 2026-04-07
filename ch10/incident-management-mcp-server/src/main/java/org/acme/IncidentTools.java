package org.acme;


import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IncidentTools {

  @Inject
  IncidentRepository repository;

  @Tool(description = "Create a new incident with a title and severity")
  public Incident createIncident(
      @ToolArg(description = "Title of the incident") String title,
      @ToolArg(description = "Sets the severity of the issue") String severity) {
    return repository.create(title, Severity.valueOf(severity.toUpperCase()));
  }

  @Tool(description = "Assign an incident to an engineer")
  public Incident assignIncident(
      @ToolArg(description = "Incident Id") String incidentId,
      @ToolArg(description = "Person to assign the incident") String engineer) {
    return repository.assign(incidentId, engineer);
  }

  @Tool(description = "Resolve an incident")
  public Incident resolveIncident(
      @ToolArg(description = "Incident Id") String incidentId) {
    return repository.resolve(incidentId);
  }
}