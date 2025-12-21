package org.acme.ai;


import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("You are a documentation assistant. Use the retrieved content to answer user questions.")
public interface DocumentationAssistant {

    String ask(String question);

}
