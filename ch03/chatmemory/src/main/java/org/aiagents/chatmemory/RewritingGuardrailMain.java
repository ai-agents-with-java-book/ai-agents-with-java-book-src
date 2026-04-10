package org.aiagents.chatmemory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class RewritingGuardrailMain {

    interface ChatBot {
        String chat(String question);
    }

    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .inputGuardrails(new StopWordsGuardrail("bomb", "gun", "rifle", "knife", "explosive"),
                        new AnonymizerGuardrail("IBM", "Microsoft", "Oracle"))
                .build();

        System.out.println(chatBot.chat("I'm Mario and I work for IBM."));
        System.out.println(chatBot.chat("For which company do I work?"));
    }
}
