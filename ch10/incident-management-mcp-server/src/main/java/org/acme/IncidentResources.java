package org.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IncidentResources {

  @Inject
  IncidentRepository repository;

  @Inject
  ObjectMapper mapper;

  @Resource(name = "incident-list",
      description = "List of all current incidents",
      uri = "incidents://incident-list")
  public TextResourceContents getAllIncidents() throws JsonProcessingException {
    return TextResourceContents
        .create(
            "incidents://incident-list",
            mapper.writeValueAsString(repository.findAll())
        );
  }

  @ResourceTemplate(name = "incident-detail",
      description = "Get incident by ID",
      uriTemplate = "incidents://incident-detail/{id}")
  public TextResourceContents getIncident(String id) throws JsonProcessingException {
    Incident incident = repository.findById(id).orElseThrow();
    return TextResourceContents.create(
        "incidents://incident-detail/" + id,
        mapper.writeValueAsString(incident)
    );
  }
}
