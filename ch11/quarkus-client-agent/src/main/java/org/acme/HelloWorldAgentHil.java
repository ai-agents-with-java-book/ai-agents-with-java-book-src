package org.acme;

import dev.langchain4j.agentic.declarative.A2AClientAgent;

public interface HelloWorldAgentHil {

    @A2AClientAgent(a2aServerUrl = "http://localhost:7777", outputKey = "response")
    String greeting(String greeting);

}
