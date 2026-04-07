package org.acme;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

@RegisterAiService
public interface ExchangeRateAssistant {

  @SystemMessage("""
        You are a helpful assistant that 
        knows how to convert currencies.
        """)
  @McpToolBox("exchangerate")  // Enable tools from the "filesystem" MCP server
  String chat(@UserMessage String message);
}
