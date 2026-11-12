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
    void generatedSchemaUsesTheExplicitOptionalTimeZone() {
        var specifications = ToolSpecifications.toolSpecificationsFrom(Clock.class);
        assertEquals(1, specifications.size());
        var specification = specifications.getFirst();
        assertEquals("getNow", specification.name());
        assertEquals(Set.of("timeZone"), specification.parameters().properties().keySet());
        assertFalse(specification.parameters().required().contains("timeZone"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "Europe/Berlin")
    void reflectiveExecutorBindsTheOptionalTimeZone(String timeZone) throws Exception {
        var clock = new RecordingClock();
        var executor = new DefaultToolExecutor(clock, Clock.class.getMethod("getNow", String.class));

        assertEquals("clock-result", executor.execute(request(timeZone), "test-memory"));
        assertEquals(1, clock.calls);
        assertEquals(timeZone, clock.lastTimeZone);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "Europe/Berlin")
    void lowLevelFlowBindsTheOptionalTimeZoneAndReturnsTheToolResult(String timeZone) throws Exception {
        var clock = new RecordingClock();
        List<ChatRequest> requests = new ArrayList<>();
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                requests.add(request);
                return ChatResponse.builder()
                        .aiMessage(requests.size() == 1
                                ? AiMessage.from(request(timeZone))
                                : AiMessage.from("The tool returned clock-result"))
                        .build();
            }
        };

        LowLevelToolMainApp.run(clock, model);

        assertEquals(1, clock.calls);
        assertEquals(timeZone, clock.lastTimeZone);
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

    private static ToolExecutionRequest request(String timeZone) {
        return ToolExecutionRequest.builder()
                .id("clock-call")
                .name("getNow")
                .arguments(timeZone == null ? "{}" : "{\"timeZone\":\"Europe/Berlin\"}")
                .build();
    }

    // Use the production Clock method's annotation and record what reaches its receiver.
    public static class RecordingClock extends Clock {
        String lastTimeZone;
        int calls;

        @Override
        public String getNow(String timeZone) {
            calls++;
            lastTimeZone = timeZone;
            return "clock-result";
        }
    }
}
