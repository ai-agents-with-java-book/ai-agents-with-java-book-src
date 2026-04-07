package org.acme;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptArg;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IncidentPrompts {

  @Prompt(name = "incident-summary",
      description = "Summarize an incident for executives")
  public PromptMessage summarizeIncident(
      @PromptArg(description = "Title of the incident") String title,
      @PromptArg(description = "The severity of the incident") String severity,
      @PromptArg(description = "The status of the incident") String status) {
    String prompt = """
            You are an SRE assistant.

            Summarize the following incident in 
            a concise way for executives:

            Title: %s
            Severity: %s
            Status: %s

            Focus on business impact and current state.
            """.formatted(title, severity, status);

    return PromptMessage.withUserRole(prompt);

  }

}
