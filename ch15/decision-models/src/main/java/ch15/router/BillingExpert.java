package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface BillingExpert {

    @UserMessage("""
            You are a member of the billing team of a software company.
            Write a short, courteous reply to the following ticket,
            with concrete steps when possible.
            The ticket is: {{ticket}}
            """)
    @Agent(name = "billing",
            description = "Handles tickets about payments, invoices and refunds",
            outputKey = "billingAnswer")
    String answer(@V("ticket") String ticket);
}
