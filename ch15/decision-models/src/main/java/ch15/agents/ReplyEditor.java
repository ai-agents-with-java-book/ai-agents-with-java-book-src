package ch15.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ReplyEditor {

    @UserMessage("""
            Improve the following reply to a support ticket so that it fully solves the customer's
            problem with clear and complete instructions, keeping it short and courteous.
            Return only the improved reply.
            The ticket is: {{ticket}}
            The reply is: {{reply}}
            """)
    @Agent(value = "Improves the reply to a ticket", outputKey = "reply")
    String edit(@V("ticket") String ticket, @V("reply") String reply);
}
