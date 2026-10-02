package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class MainApp {
    public static void main(String[] args) {
        
            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();

            Assistant assistant = AiServices
                .builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new Clock())
                .build();

            String r = assistant
                .assist("I am the user id 1234 and I'd love to know my current date and time");
            
            System.out.println(r);

    }

    interface Assistant {
        String assist(String query);
    }
}
