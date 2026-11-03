package ch15.cascade;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

public interface SupportCascade {

    @Agent
    ResultWithAgenticScope<String> ask(@MemoryId String ticketId,
                                       @V("request") String request);
}
