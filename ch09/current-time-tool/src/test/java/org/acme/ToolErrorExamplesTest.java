package org.acme;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolErrorExamplesTest {
    private static final String MARKER = "private-diagnostic-marker";

    @Test
    void handlesUnknownTool() {
        exercise(ToolErrorExamples::unknownTool, "missing", "{}", false, false);
    }

    @Test
    void handlesInvalidArgumentsWithoutExposingDetails() {
        exercise(ToolErrorExamples::invalidArguments, "getNow",
                "{\"timeZone\":\"" + MARKER + "\"", true, false);
    }

    @Test
    void handlesExecutionFailureWithoutExposingDetails() {
        exercise(ToolErrorExamples::executionFailure, "getNow",
                "{\"timeZone\":\"" + MARKER + "\"}", true, true);
    }

    private void exercise(Function<ChatModel, ToolErrorExamples.Assistant> factory,
            String name, String arguments, boolean diagnostic, boolean expectMarkerInDiagnostics) {
        List<ChatRequest> calls = new ArrayList<>();
        List<LogRecord> diagnostics = new ArrayList<>();
        Logger logger = Logger.getLogger(ToolFailures.class.getName());
        Handler capture = new Handler() {
            public void publish(LogRecord record) { diagnostics.add(record); }
            public void flush() { }
            public void close() { }
        };
        logger.addHandler(capture);
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                calls.add(request);
                return ChatResponse.builder().aiMessage(calls.size() == 1
                        ? AiMessage.from(ToolExecutionRequest.builder().id("error-call")
                            .name(name).arguments(arguments).build())
                        : AiMessage.from("handled")).build();
            }
        };
        try {
            assertEquals("handled", factory.apply(model).assist("current time"));
            assertEquals(2, calls.size());
            var result = assertInstanceOf(ToolExecutionResultMessage.class,
                    calls.get(1).messages().getLast());
            assertEquals("error-call", result.id());
            assertFalse(result.text().contains(MARKER));
            if (diagnostic) {
                String reference = result.text().split("Reference: ")[1];
                assertTrue(diagnostics.stream().anyMatch(r ->
                        r.getMessage().contains(reference) && r.getThrown() != null));
                if (expectMarkerInDiagnostics) assertTrue(diagnostics.stream().anyMatch(r -> {
                    for (Throwable e = r.getThrown(); e != null; e = e.getCause()) {
                        if (String.valueOf(e.getMessage()).contains(MARKER)) return true;
                    }
                    return false;
                }));
            }
        } finally {
            logger.removeHandler(capture);
        }
    }
}
