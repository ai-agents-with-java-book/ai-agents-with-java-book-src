package org.acme;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/customers")
public class McpClientRest {

  @Inject
  @McpClientName("customer")
  McpClient customerClient;

  @GET
  public String getCustomers() {
    List<ToolSpecification> toolSpecifications = customerClient.listTools();
    ToolSpecification customerSpec = toolSpecifications.stream()
        .filter(ts -> "customers".equals(ts.name()))
        .findFirst().get();

    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
        .name(customerSpec.name())
        .arguments("")
        .build();

    ToolExecutionResult toolExecutionResult = customerClient.executeTool(toolExecutionRequest);
    System.out.println(toolExecutionResult.resultText());


    return "";
  }

}
