package ch15.cascade;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FaqAgent {

    @UserMessage("""
            You are the FAQ assistant of the Ledgerly invoicing application.
            Answer the following customer request in no more than 3 sentences,
            using only your knowledge of Ledgerly: creating and sending invoices,
            recurring billing, exporting data and managing users.
            If the request is not about Ledgerly, or you are not sure, answer exactly:
            "I can only help with questions about Ledgerly."
            The request is: {{request}}
            """)
    @Agent(description = "Answers frequently asked questions about Ledgerly",
            outputKey = "faqAnswer")
    String faq(@V("request") String request);
}
