package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface SupportPipeline {

    @Agent
    String reply(@V("ticket") String ticket);
}
