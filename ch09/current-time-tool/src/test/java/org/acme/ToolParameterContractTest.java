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

    @Test
    void batchesResultsAndPreservesEveryTurn() throws Exception {
        var clock = new RecordingClock();
        List<ChatRequest> calls = new ArrayList<>();
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                calls.add(request);
                AiMessage answer = switch (calls.size()) {
                    case 1 -> AiMessage.from(call("berlin", "getNow"),
                            call("tokyo", "getNow"));
                    case 2 -> AiMessage.from(call("utc", "getNow"));
                    default -> AiMessage.from("finished");
                };
                return ChatResponse.builder().aiMessage(answer).build();
            }
        };
        assertEquals("finished", LowLevelToolMainApp.run(clock, model));
        assertEquals(3, clock.calls);
        assertEquals(List.of("Europe/Berlin", "Asia/Tokyo", "Europe/Berlin"),
                clock.timeZones);
        assertEquals(List.of(1, 4, 6), calls.stream()
                .map(c -> c.messages().size()).toList());
        assertEquals(calls.get(1).messages(), calls.get(2).messages().subList(0, 4));
        assertEquals(List.of("berlin", "tokyo", "utc"), calls.get(2).messages().stream()
                .filter(ToolExecutionResultMessage.class::isInstance)
                .map(ToolExecutionResultMessage.class::cast)
                .map(ToolExecutionResultMessage::id).toList());
    }

    @Test
    void returnsAnAnswerWithoutCallingATool() throws Exception {
        var clock = new RecordingClock();
        int[] calls = {0};
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                calls[0]++;
                return ChatResponse.builder().aiMessage(AiMessage.from("hello")).build();
            }
        };
        assertEquals("hello", LowLevelToolMainApp.run(clock, model));
        assertEquals(1, calls[0]);
        assertEquals(0, clock.calls);
    }

    @Test
    void returnsAnUnknownToolResultWithTheRequestId() throws Exception {
        var clock = new RecordingClock();
        List<ChatRequest> calls = new ArrayList<>();
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                calls.add(request);
                return ChatResponse.builder().aiMessage(calls.size() == 1
                        ? AiMessage.from(call("unknown", "missing"))
                        : AiMessage.from("no tool")).build();
            }
        };
        assertEquals("no tool", LowLevelToolMainApp.run(clock, model));
        var result = assertInstanceOf(ToolExecutionResultMessage.class,
                calls.get(1).messages().getLast());
        assertEquals("unknown", result.id());
        assertEquals("Unknown tool. Use an offered tool.", result.text());
        assertEquals(0, clock.calls);
    }

    @Test
    void stopsAtTheToolTurnLimit() {
        var clock = new RecordingClock();
        int[] calls = {0};
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                calls[0]++;
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(call("again", "getNow"))).build();
            }
        };
        assertEquals("Tool-turn limit reached", assertThrows(IllegalStateException.class,
                () -> LowLevelToolMainApp.run(clock, model)).getMessage());
        assertEquals(LowLevelToolMainApp.MAX_TOOL_TURNS, calls[0]);
        assertEquals(LowLevelToolMainApp.MAX_TOOL_TURNS, clock.calls);
    }

    private static ToolExecutionRequest call(String id, String name) {
        String zone = "tokyo".equals(id) ? "Asia/Tokyo" : "Europe/Berlin";
        return ToolExecutionRequest.builder().id(id).name(name)
                .arguments("{\"timeZone\":\"" + zone + "\"}").build();
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
        List<String> timeZones = new ArrayList<>();

        @Override
        public String getNow(String timeZone) {
            calls++;
            lastTimeZone = timeZone;
            timeZones.add(timeZone);
            return "clock-result";
        }
    }
}
