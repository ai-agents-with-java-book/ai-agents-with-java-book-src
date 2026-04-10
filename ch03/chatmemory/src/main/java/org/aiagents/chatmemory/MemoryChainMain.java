package org.aiagents.chatmemory;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;

public class MemoryChainMain {

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ConversationalChain chain = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();

        chat(chain, "Hi, my name is Mario", "What is my name?");
    }

    private static void chat(ConversationalChain chain, String... questions) {
        for (String question : questions) {
            System.out.println("Q: " + question);
            String answer = chain.execute(question);
            System.out.println("A: " + answer);
        }
    }
}
