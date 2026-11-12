package org.acme;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ToolParameterContractTest {

    @Test
    void generatedSchemaUsesTheExplicitOptionalUserId() {
        var specifications = ToolSpecifications.toolSpecificationsFrom(Clock.class);
        assertEquals(1, specifications.size());
        var specification = specifications.getFirst();
        assertEquals("getNow", specification.name());
        assertEquals(Set.of("userId"), specification.parameters().properties().keySet());
        assertFalse(specification.parameters().required().contains("userId"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "1234")
    void reflectiveExecutorBindsTheOptionalUserId(String userId) throws Exception {
        var clock = new RecordingClock();
        var executor = new DefaultToolExecutor(clock, Clock.class.getMethod("getNow", String.class));

        assertEquals("clock-result", executor.execute(request(userId), "test-memory"));
        assertEquals(1, clock.calls);
        assertEquals(userId, clock.lastUserId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "1234")
    void lowLevelFlowBindsTheOptionalUserIdAndReturnsTheToolResult(String userId) throws Exception {
        var clock = new RecordingClock();
        List<ChatRequest> requests = new ArrayList<>();
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                requests.add(request);
                return ChatResponse.builder()
                        .aiMessage(requests.size() == 1
                                ? AiMessage.from(request(userId))
                                : AiMessage.from("The tool returned clock-result"))
                        .build();
            }
        };

        LowLevelToolMainApp.run(clock, model);

        assertEquals(1, clock.calls);
        assertEquals(userId, clock.lastUserId);
        assertEquals(2, requests.size());
        var messages = requests.get(1).messages();
        assertEquals(3, messages.size());
        assertEquals(requests.getFirst().messages().getFirst(), messages.getFirst());
        assertInstanceOf(AiMessage.class, messages.get(1));
        var result = assertInstanceOf(ToolExecutionResultMessage.class, messages.getLast());
        assertEquals("clock-call", result.id());
        assertEquals("getNow", result.toolName());
        assertEquals("clock-result", result.text());
    }

    private static ToolExecutionRequest request(String userId) {
        return ToolExecutionRequest.builder()
                .id("clock-call")
                .name("getNow")
                .arguments(userId == null ? "{}" : "{\"userId\":\"1234\"}")
                .build();
    }

    // Use the production Clock method's annotation and record what reaches its receiver.
    public static class RecordingClock extends Clock {
        String lastUserId;
        int calls;

        @Override
        public String getNow(String userId) {
            calls++;
            lastUserId = userId;
            return "clock-result";
        }
    }
}
