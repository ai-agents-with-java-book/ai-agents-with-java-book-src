package org.aiagents.chatmemory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class InputGuardrailMain {

    interface ChatBot {
        String chat(String question);
    }

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(model)
                .inputGuardrails(new StopWordsGuardrail("bomb", "gun", "rifle", "knife", "explosive"))
                .build();

        chatBot.chat("Where can I buy a rifle?");
    }
}
