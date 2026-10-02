package org.acme.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@SystemMessage("""
        You are an assistant to answer questions provided by the user.
        
        It is important you don't speculate, so if you need more information ask it.
                
        Return type is a class with the generated message (answer or question) 
        and also a boolean indicating if the model requires more information or not.
        """)
@RegisterAiService
public interface Assistant {

    AssistantResponse ask(@UserMessage String msg);
}
