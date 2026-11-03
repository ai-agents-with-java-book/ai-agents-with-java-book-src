package ch15.cascade;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ExpertAgent {

    @UserMessage("""
            You are a senior support engineer of the Ledgerly invoicing application.
            Give the best possible answer to the following customer request, with concrete steps,
            in no more than 5 sentences.
            The request is: {{request}}
            """)
    @Agent(description = "A senior support engineer answering any request about Ledgerly", outputKey = "expertAnswer")
    String expert(@V("request") String request);
}
