package org.acme.repair;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static org.acme.repair.Repair.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelPlannerTest {
    @Test void runsTheLoopThroughTheRealAiServiceAndAStubProvider() throws Exception {
        var request = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String proposal = """
                    {"action":"REPLACE_SOURCE","plan":["Fix the sum"],
                     "replacementSource":"fixed source","reason":"Observed sum failure"}
                    """;
            String response = """
                    {"id":"stub","object":"chat.completion","created":1,"model":"stub",
                     "choices":[{"index":0,"message":{"role":"assistant","content":%s},
                     "finish_reason":"stop"}],
                     "usage":{"prompt_tokens":10,"completion_tokens":10,"total_tokens":20}}
                    """.formatted(quote(proposal));
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var model = OpenAiChatModel.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                    .apiKey("stub-key").modelName("stub")
                    .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA).strictJsonSchema(true)
                    .maxRetries(0).timeout(Duration.ofSeconds(5)).build();
            var planner = new ModelPlanner(AiServices.create(RepairService.class, model));
            Verifier verifier = (source, duration) -> new Observation(
                    source.equals("fixed source") ? Check.PASSED : Check.REJECTED,
                    Set.of(), "Sum failure", "stub");
            var result = new RepairLoop(planner, verifier,
                    new Limits(2, 2, Duration.ofSeconds(20))).run("Repair the sum", "original");
            assertEquals(Status.COMPLETED, result.status(), result.reason());
            assertEquals("fixed source", result.bestSource());
            assertTrue(request.get().contains("json_schema"));
            assertTrue(request.get().contains("replacementSource"));
            assertTrue(request.get().contains("Sum failure"));
        } finally {
            server.stop(0);
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
