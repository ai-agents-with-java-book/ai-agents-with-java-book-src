package org.acme;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;

public class MainClient {

    public static void main(String args[]) throws InterruptedException {
        McpTransport transport = StreamableHttpMcpTransport.builder()
            .url("http://localhost:8080/mcp")
            .logRequests(true) // if you want to see the traffic in the log
            .logResponses(true)
            .build();

        McpClient mcpClient = DefaultMcpClient.builder()
            .key("MyMCPClient")
            .transport(transport)
            .onResourceUpdated((client, uri) -> {
                // re-read the updated resource
                McpReadResourceResult result = client.readResource(uri);
                System.out.println("URI %s updated".formatted(uri));
            })
            .build();

        mcpClient.subscribeToResource("http://stocks/IBM");

        McpReadResourceResult mcpReadResourceResult = mcpClient.readResource("http://stocks/IBM");
        System.out.println(((McpTextResourceContents) mcpReadResourceResult.contents().get(0)).text());

        Thread.sleep(60000);
    }

}
