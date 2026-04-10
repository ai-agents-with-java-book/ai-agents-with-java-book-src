package org.aiagents.chatmemory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public class MemoryIdAiServiceMain {

    interface ChatBot {
        String chat(@MemoryId Integer memoryId, @UserMessage String question);
    }

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .build())
                .build();

        chat(chatBot, 1, "Hi, my name is Mario");
        chat(chatBot, 2, "Hi, my name is Alex");
        chat(chatBot, 2, "What is my name?");
        chat(chatBot, 1, "What is my name?");
    }

    private static void chat(ChatBot chatBot, Integer memoryId, String question) {
        System.out.println("Q" + memoryId + ": " + question);
        String answer = chatBot.chat(memoryId, question);
        System.out.println("A" + memoryId + ": " + answer);
    }
}
