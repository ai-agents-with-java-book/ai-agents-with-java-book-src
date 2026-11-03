package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TechnicalExpert {

    @UserMessage("""
            You are a member of the technical support team of a software company.
            Write a short, courteous reply to the following ticket,
            with concrete steps when possible.
            The ticket is: {{ticket}}
            """)
    @Agent(name = "technical",
            description = "Handles tickets about errors and bugs in the product",
            outputKey = "technicalAnswer")
    String answer(@V("ticket") String ticket);
}
