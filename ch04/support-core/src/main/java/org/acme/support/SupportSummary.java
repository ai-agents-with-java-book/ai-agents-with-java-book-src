package org.acme.support;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SupportSummary {
    @SystemMessage("""
        Summarize the customer's support request in two sentences.
        Keep order identifiers exactly as provided.
        If information is missing, say what is missing.
        Do not claim that an order was changed or a shipment created.
        """)
    String summarize(@UserMessage String request);
}
