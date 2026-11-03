package ch15.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;

public interface EscalationGate {

    @Decide("""
            Does this ticket need a human support agent today?
            Yes: the customer is locked out, loses money or threatens to leave,
            or the ticket matches the urgency rules.
            No: a question that the knowledge base can answer,
            or a request that can wait.""")
    @Agent(value = "Checks whether a ticket needs a human support agent today",
            outputKey = "needsHuman")
    boolean check(@V("ticket") String ticket, @V("urgencyRules") String urgencyRules);
}
