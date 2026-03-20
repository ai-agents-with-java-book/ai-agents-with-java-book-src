package org.acme;


import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MainApp {
    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(MainApp.class);

        McpTransport transport = StdioMcpTransport.builder()
                .command(List.of("npx", "-y", "@karashiiro/exchange-rate-mcp"))
                .build();

        McpClient mcpClient = DefaultMcpClient.builder()
                .key("Finance MCP")
                .transport(transport)
                .build();

        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        System.out.println(assistant.chat("10 euros in dollars?"));

    }
}
