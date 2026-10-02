package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class LowLevelToolMainApp {
    
    public static void main(String[] args) throws NoSuchMethodException, SecurityException, JsonMappingException, JsonProcessingException {
        
            Clock clock = new Clock();

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();

            List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(Clock.class);

            UserMessage userMessage = UserMessage.from("I am the user id 1234 and I'd love to know my current date and time");
            ChatRequest request = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
            
            ChatResponse response = chatModel.chat(request);

            AiMessage aiMessage = response.aiMessage();

            ObjectMapper mapper = new ObjectMapper();

            if (aiMessage.hasToolExecutionRequests()) {
                List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
                for (ToolExecutionRequest toolExecutionRequest : toolExecutionRequests) {
                    if("getNow".equals(toolExecutionRequest.name())) {
                        
                        String arguments = toolExecutionRequest.arguments();
                        System.out.println(arguments);
                        Map<String, String> parsedArguments = mapper.readValue(arguments, Map.class);
                        
                        String result = clock.getNow(parsedArguments.get("arg0"));

                        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);
                        ChatRequest request2 = ChatRequest.builder()
                                .messages(List.of(userMessage, aiMessage, toolExecutionResultMessage))
                                .toolSpecifications(toolSpecifications)
                                .build();
                        
                        ChatResponse response2 = chatModel.chat(request2);
                        System.out.println(response2.aiMessage().text());


                    }
                }
            }

    }

}
