package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.lang.reflect.Method;
import java.util.Map;

import dev.langchain4j.agent.tool.ToolSpecification;


public class DynamicToolsMainApp {
    

    public static void main(String[] args) throws NoSuchMethodException, SecurityException {
        
            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();

            ToolSpecification toolSpecification = ToolSpecification.builder()
                                                        .name("getNow")
                                                        .description("""
                                                            Gets the current date and time in the user's timezone. 
                                                            The current date and time is returned in ISO-8601 format
                                                        """)
                                                        .parameters(JsonObjectSchema.builder()
                                                                    .addStringProperty("userId", "The user id")
                                                                    .build()
                                                        )
                                                        .build();
            
            Clock tools = new Clock();
            Method method = Clock.class.getMethod("getNow", String.class);
            ToolExecutor toolExecutor = new DefaultToolExecutor(tools, method);


            ToolProvider toolProvider = (toolProviderRequest) -> {
                if (toolProviderRequest.userMessage().singleText().contains("current")) {
                    return ToolProviderResult.builder()
                        .add(toolSpecification, toolExecutor)
                        .build();
                } else {
                    return null;
                }
            };

            Assistant assistant = AiServices
                .builder(Assistant.class)
                .chatModel(chatModel)
                .toolProvider(toolProvider)
                .build();

            String r = assistant
                .assist("I am the user id 1234 and I'd love to know my current date and time");
            
            System.out.println(r);

    }

    interface Assistant {
        String assist(String query);
    }

}
