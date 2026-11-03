package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

public interface AnswerSynthesizer {

    @UserMessage("""
            Merge the replies that different support teams wrote to the same ticket
            into a single reply.
            The ticket is: {{ticket}}
            The replies, keyed by team, are: {{answers}}
            """)
    @Agent(description = "Merges the replies of several teams into one",
            outputKey = "reply")
    String synthesize(@V("ticket") String ticket,
                      @V("answers") Map<String, String> answers);
}
