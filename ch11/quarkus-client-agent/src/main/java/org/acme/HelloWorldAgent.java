package org.acme;

import dev.langchain4j.agentic.declarative.A2AClientAgent;
import dev.langchain4j.agentic.declarative.A2AClientCustomizer;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;

public interface HelloWorldAgent {

    @A2AClientAgent(a2aServerUrl = "http://localhost:7777", outputKey = "response")
    String greeting(String greeting);

    @A2AClientCustomizer
    static void customizer(ClientBuilder cb) {
        System.out.println("Customize Client");
        cb
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());
    }

}
