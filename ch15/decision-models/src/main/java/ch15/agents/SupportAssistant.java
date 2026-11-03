package ch15.agents;

import ch15.Team;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SupportAssistant {

    @UserMessage("""
            You are a member of the {{team}} support team of a software company.
            Write a short, courteous reply to the following ticket,
            with concrete steps when possible.
            Refund requested: {{refund}}.
            The ticket is: {{ticket}}
            """)
    @Agent(value = "Writes a reply to a support ticket", outputKey = "reply")
    String reply(@V("ticket") String ticket, @V("team") Team team,
                 @V("refund") boolean refund);
}
