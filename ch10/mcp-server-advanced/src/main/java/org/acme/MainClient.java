package org.acme;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpRoot;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.util.List;

public class MainClient {

    public interface Assistant {
        String ask(String question);
    }

    public static void main(String[] args) {

        McpTransport transport = StreamableHttpMcpTransport.builder()
                .url("http://localhost:8080/mcp")
                .build();

        McpClient mcpClient = DefaultMcpClient.builder()
                .key("MyMCPClient")
                .transport(transport)
                .build();

        McpRoot repositoriesDirectory = new McpRoot("git-repos", "file:///Users/alexsoto/git/");
        mcpClient.setRoots(List.of(repositoriesDirectory));

        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        System.out.println(assistant.ask("Get information of Git repository car-rental-bdd"));


    }

}
