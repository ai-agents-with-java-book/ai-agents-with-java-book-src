package org.aiagents.chatmemory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;

public class MemoryEvictAiServiceMain {

    interface ChatBot extends ChatMemoryAccess {
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
        chat(chatBot, 1, "What is my name?");

        ChatMemory memory1 = chatBot.getChatMemory(1);
        System.out.println(memory1.messages());

        boolean evicted = chatBot.evictChatMemory(1);
        System.out.println("Memory 1 Evicted: " + evicted);

        System.out.println(chatBot.getChatMemory(1));

        chat(chatBot, 1, "What is my name?");
    }

    private static void chat(ChatBot chatBot, Integer memoryId, String question) {
        System.out.println("Q" + memoryId + ": " + question);
        String answer = chatBot.chat(memoryId, question);
        System.out.println("A" + memoryId + ": " + answer);
    }
}
