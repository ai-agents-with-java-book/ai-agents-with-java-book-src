package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.util.List;
import java.util.ArrayList;
import dev.langchain4j.data.message.ChatMessage;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class LowLevelToolMainApp {
    
    public static void main(String[] args) throws JsonProcessingException {
        
            Clock clock = new Clock();

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();

            System.out.println(run(clock, chatModel));
    }

    static final int MAX_TOOL_TURNS = 4;

    static String run(Clock clock, ChatModel chatModel)
            throws JsonProcessingException {
        List<ToolSpecification> toolSpecifications =
                ToolSpecifications.toolSpecificationsFrom(Clock.class);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from(
                "What is the current date and time in Europe/Berlin?"));
        ObjectMapper mapper = new ObjectMapper();

        for (int turn = 0; turn < MAX_TOOL_TURNS; turn++) {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.copyOf(messages))
                    .toolSpecifications(toolSpecifications)
                    .build();
            AiMessage aiMessage = chatModel.chat(request).aiMessage();
            messages.add(aiMessage);
            if (!aiMessage.hasToolExecutionRequests()) {
                return aiMessage.text();
            }
            for (ToolExecutionRequest toolRequest
                    : aiMessage.toolExecutionRequests()) {
                String result;
                if ("getNow".equals(toolRequest.name())) {
                    Map<String, String> arguments = mapper.readValue(
                            toolRequest.arguments(), Map.class);
                    result = clock.getNow(arguments.get("timeZone"));
                } else {
                    result = "Unknown tool. Use an offered tool.";
                }
                messages.add(ToolExecutionResultMessage.from(
                        toolRequest, result));
            }
        }
        throw new IllegalStateException("Tool-turn limit reached");
    }
}
