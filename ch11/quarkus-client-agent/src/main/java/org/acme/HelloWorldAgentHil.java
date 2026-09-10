package org.acme;

import dev.langchain4j.agentic.a2a.A2AContextId;
import dev.langchain4j.agentic.a2a.A2ATaskId;
import dev.langchain4j.agentic.declarative.A2AClientAgent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

public interface HelloWorldAgentHil  {

    @A2AClientAgent(a2aServerUrl = "http://localhost:7777", outputKey = "response")
    String greeting(@A2AContextId @V("contextId") String contextId,
                    @A2ATaskId @V("taskId") String taskId, String greeting);

}
