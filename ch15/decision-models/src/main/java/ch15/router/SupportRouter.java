package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface SupportRouter {

    @Agent
    String answer(@V("ticket") String ticket);
}
