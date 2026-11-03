package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AccountExpert {

    @UserMessage("""
            You are a member of the account management team of a software company.
            Write a short, courteous reply to the following ticket,
            with concrete steps when possible.
            The ticket is: {{ticket}}
            """)
    @Agent(name = "account",
            description = "Handles tickets about login, passwords and permissions",
            outputKey = "accountAnswer")
    String answer(@V("ticket") String ticket);
}
