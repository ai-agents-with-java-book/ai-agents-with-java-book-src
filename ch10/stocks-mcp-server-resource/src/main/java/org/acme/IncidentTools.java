package org.acme;

import io.quarkiverse.mcp.server.Sampling;
import io.quarkiverse.mcp.server.SamplingMessage;
import io.quarkiverse.mcp.server.SamplingRequest;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;

public class IncidentTools {

  private static final Map<String, String>
      incidents = Map.of("web-1", """
      Over the past 48 hours, multiple users have reported being unable to proceed beyond the cart page when attempting to complete a purchase using mobile devices. The issue specifically affects the “Proceed to Checkout” button, which appears visually active but does not trigger any navigation or backend request when tapped.
      
      Initial investigation suggests that the problem is isolated to mobile environments, particularly on iOS devices using Safari and Chrome. Desktop browsers, including Chrome, Firefox, and Edge, do not exhibit this behavior, indicating that the issue may be related to mobile-specific event handling or responsive layout differences.
      
      From a user experience perspective, the issue is highly disruptive. Users can browse products and add items to their cart without any problems, but are effectively blocked at the final step of the conversion funnel. This creates a false sense of functionality, as there are no visible error messages, warnings, or fallback mechanisms to guide the user or explain the failure.
      
      Preliminary analysis points to a potential frontend regression introduced in the latest deployment (v2.3.1). Recent changes included updates to the cart component, refactoring of click event handlers, and CSS adjustments for mobile responsiveness. There is a possibility that an invisible overlay element, incorrect z-index layering, or a misconfigured event listener is preventing the button from receiving or processing touch events correctly.
      
      Additionally, no relevant errors are being logged in the browser console, which suggests that the failure may be occurring silently at the UI interaction level rather than in the application logic or API layer. This makes the issue harder to detect through standard monitoring and requires manual reproduction on affected devices.
      
      Given that this issue directly impacts the checkout flow, it represents a critical blocker for revenue generation on mobile platforms. Immediate investigation and resolution are strongly recommended, along with the addition of better client-side logging or error handling to prevent similar silent failures in the future.
      """);

  @Inject
  Logger logger;

  @Tool(description = "Gets a summary of an incident")
  public String advancedSampling(@ToolArg(description = "Issue identification") String issueId,
                                 Sampling sampling) {

    String description = incidents.get(issueId);

    if (sampling.isSupported()) {

      logger.info("Sampling Supported");

      return sampling.requestBuilder()
          .setMaxTokens(200)
          .setSystemPrompt("You are an assistant that summarizes text into three bullet points.")
          .addMessage(SamplingMessage.withUserRole(description))
          .setIncludeContext(SamplingRequest.IncludeContext.THIS_SERVER)
          .build()
          .sendAndAwait()
          .content().asText().text();
    }

    return description;
  }
}
