package ch15.agents;

import ch15.Team;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;

public interface TicketTriage {

    record Triage(
            @Decide("Which team should handle this ticket?") Team team,
            @Decide("Does the customer ask for money back?") boolean refund) {}

    @Agent(description = "Assigns a ticket to a team and detects refund requests",
            outputKey = "triage")
    Triage triage(@V("ticket") String ticket);
}
