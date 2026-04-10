package org.aiagents.chatmemory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class OutputGuardrailMain {
    interface ChatBot {
        String chat(String question);
    }

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(model)
                .outputGuardrails(new UppercaseOutputGuardrail())
                .build();

        System.out.println(chatBot.chat("My name is Mario."));
    }
}
