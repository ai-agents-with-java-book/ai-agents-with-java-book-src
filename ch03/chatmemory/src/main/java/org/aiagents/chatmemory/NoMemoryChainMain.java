package org.aiagents.chatmemory;

import dev.langchain4j.model.chat.ChatModel;

public class NoMemoryChainMain {

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        chat(model, "Hi, my name is Mario", "What is my name?");
    }

    private static void chat(ChatModel model, String... questions) {
        for (String question : questions) {
            System.out.println("Q: " + question);
            String answer = model.chat(question);
            System.out.println("A: " + answer);
        }
    }
}
