package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.util.Map;

public interface MultiSupportRouter {

    @Agent
    Map<String, String> answer(@V("ticket") String ticket);
}
