package org.aiagents.chatmemory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class MemoryAiServiceMain {

    interface ChatBot {
        String chat(String question);
    }

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

        chat(chatBot, "Hi, my name is Mario", "What is my name?");
    }

    private static void chat(ChatBot chatBot, String... questions) {
        for (String question : questions) {
            System.out.println("Q: " + question);
            String answer = chatBot.chat(question);
            System.out.println("A: " + answer);
        }
    }
}
