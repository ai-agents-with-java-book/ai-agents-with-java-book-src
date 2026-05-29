package org.aiagents.observability;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.SessionScoped;

@SessionScoped
@RegisterAiService
public interface MedicalExpertAgent {

    @SystemMessage("""
            You are a medical expert.
            You are friendly, polite and concise.
            Analyze the user request under a medical point of view and provide the best possible answer.
            """)
    String chat(@MemoryId String memoryId, @UserMessage String request);
}
