package ch15.agents;

import ch15.Quality;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;
import dev.langchain4j.service.decision.Scale;

public interface ReplyGrader {

    @Decide("How well does the reply solve the problem described in the ticket?")
    @Agent(value = "Grades the reply to a ticket", outputKey = "replyQuality")
    Scale<Quality> grade(@V("ticket") String ticket, @V("reply") String reply);
}
