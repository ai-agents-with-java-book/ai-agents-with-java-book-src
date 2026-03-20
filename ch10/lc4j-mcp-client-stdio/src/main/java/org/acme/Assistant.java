package org.acme;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {

    @SystemMessage("""
            ChatBot to help user answering questions about exchange rate between currencies.
            
            Use the MCP to answer questions regarding currencies.
            """)
    String chat(String msg);

}
