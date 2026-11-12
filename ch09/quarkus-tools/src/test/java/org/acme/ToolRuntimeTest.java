package org.acme;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ToolRuntimeTest.Provider.class)
class ToolRuntimeTest {
    @Inject MyAiService assistant;
    @Inject DynamicToolProviderSupplier supplier;
    @Inject HotelCache cache;
    @Inject HotelTools hotels;
    @Inject RedisDataSource redis;
    @ConfigProperty(name = "test.provider.port") int port;
    private final ObjectMapper json = new ObjectMapper();

    public static class Provider implements QuarkusTestResourceLifecycleManager {
        WireMockServer server;
        public Map<String, String> start() {
            server = new WireMockServer(options().dynamicPort());
            server.start();
            return Map.of(
                    "quarkus.langchain4j.openai.base-url", server.baseUrl() + "/v1",
                    "quarkus.langchain4j.openai.api-key", "test-key",
                    "quarkus.langchain4j.openai.chat-model.model-name", "test-model",
                    "quarkus.rest-client.hotel-catalog.url", server.baseUrl(),
                    "test.provider.port", Integer.toString(server.port()));
        }
        public void stop() { if (server != null) server.stop(); }
    }

    @BeforeEach
    void reset() {
        configureFor("localhost", port);
        resetAllRequests();
        removeAllMappings();
        clearCities();
    }

    @AfterEach
    void clearCities() {
        redis.key().del(HotelCache.key("Kyoto"), HotelCache.key("Berlin"),
                HotelCache.key("Missing"));
    }

    @Test
    void selectsAndExecutesTheOptionalClockTool() throws Exception {
        assertNotNull(supplier.get());
        assertSame(supplier.get(), supplier.get());
        toolCall("{}");
        assertEquals("handled", assistant.assist("current time"));
        var calls = findAll(postRequestedFor(urlEqualTo("/v1/chat/completions")));
        assertEquals(2, calls.size());
        JsonNode first = calls.stream().map(r -> read(r.getBodyAsString()))
                .filter(r -> toolResult(r) == null).findFirst().orElseThrow();
        var parameters = first.path("tools").get(0).path("function").path("parameters");
        assertTrue(parameters.path("properties").has("timeZone"));
        assertFalse(parameters.path("properties").has("arg0"));
        for (JsonNode required : parameters.path("required")) {
            assertNotEquals("timeZone", required.asText());
        }
        String result = calls.stream().map(r -> toolResult(read(r.getBodyAsString())))
                .filter(java.util.Objects::nonNull).findFirst().orElseThrow();
        assertDoesNotThrow(() -> java.time.ZonedDateTime.parse(result.replace("\"", "")));
    }

    @Test
    void omitsTheClockWhenTheSelectionWordIsAbsent() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(okJson(answer("handled"))));
        assertEquals("handled", assistant.assist("hello"));
        var calls = findAll(postRequestedFor(urlEqualTo("/v1/chat/completions")));
        assertEquals(1, calls.size());
        JsonNode request = read(calls.getFirst().getBodyAsString());
        assertTrue(request.path("tools").isMissingNode() || request.path("tools").isEmpty());
    }

    @Test
    void sanitizesArgumentFailuresThroughTheAiService() {
        assertSanitized("{\"timeZone\":\"private-marker\"");
    }

    @Test
    void sanitizesExecutionFailuresThroughTheAiService() {
        assertSanitized("{\"timeZone\":\"private-marker\"}");
    }

    private void assertSanitized(String arguments) {
        toolCall(arguments);
        assertEquals("handled", assistant.assist("current time"));
        String result = findAll(postRequestedFor(urlEqualTo("/v1/chat/completions")))
                .stream().map(r -> toolResult(read(r.getBodyAsString())))
                .filter(java.util.Objects::nonNull).findFirst().orElseThrow();
        assertFalse(result.contains("private-marker"));
        assertTrue(result.contains("Reference: "));
    }

    @Test
    void fallbackKeepsCityAndStoredTimestamp() {
        Instant kyotoTime = Instant.parse("2026-01-10T09:00:00Z");
        Instant berlinTime = kyotoTime.plusSeconds(60);
        cache.store(" Kyoto ", List.of(new Hotel("Kyoto Inn")), kyotoTime);
        cache.store("BERLIN", List.of(new Hotel("Berlin Inn")), berlinTime);
        stubFor(get(urlPathEqualTo("/hotels")).willReturn(serverError()));
        assertEquals(new HotelSuggestions(List.of(new Hotel("Kyoto Inn")),
                "cache", kyotoTime), hotels.searchHotels("kyoto"));
        assertEquals(new HotelSuggestions(List.of(new Hotel("Berlin Inn")),
                "cache", berlinTime), hotels.searchHotels("Berlin"));
        assertEquals(new HotelSuggestions(List.of(), "cache-miss", null),
                hotels.searchHotels("Missing"));
    }

    @Test
    void liveSearchPopulatesTheSameCityKey() {
        stubFor(get(urlPathEqualTo("/hotels"))
                .willReturn(okJson("[{\"name\":\"Kyoto Inn\"}]")));
        HotelSuggestions live = hotels.searchHotels("Kyoto");
        assertEquals("live", live.source());
        assertEquals(List.of(new Hotel("Kyoto Inn")), live.hotels());
        HotelSuggestions cached = cache.suggestions(" KYOTO ");
        assertEquals("cache", cached.source());
        assertEquals(live.hotels(), cached.hotels());
        assertEquals(live.asOf(), cached.asOf());
    }

    private void toolCall(String arguments) {
        String message;
        try {
            message = json.writeValueAsString(Map.of("role", "assistant", "tool_calls",
                    List.of(Map.of("id", "clock-call", "type", "function", "function",
                            Map.of("name", "getNow", "arguments", arguments)))));
        } catch (Exception e) { throw new AssertionError(e); }
        stubFor(post(urlEqualTo("/v1/chat/completions")).atPriority(10)
                .willReturn(okJson(envelope(message, "tool_calls"))));
        stubFor(post(urlEqualTo("/v1/chat/completions")).atPriority(1)
                .withRequestBody(matchingJsonPath("$.messages[?(@.role == 'tool')]"))
                .willReturn(okJson(answer("handled"))));
    }

    private JsonNode read(String text) {
        try { return json.readTree(text); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private static String toolResult(JsonNode request) {
        for (JsonNode message : request.path("messages")) {
            if ("tool".equals(message.path("role").asText())) {
                assertEquals("clock-call", message.path("tool_call_id").asText());
                return message.path("content").asText();
            }
        }
        return null;
    }

    private static String answer(String text) {
        return envelope("{\"role\":\"assistant\",\"content\":\"" + text + "\"}", "stop");
    }

    private static String envelope(String message, String reason) {
        return "{\"id\":\"response\",\"object\":\"chat.completion\",\"created\":0,"
                + "\"model\":\"test-model\",\"choices\":[{\"index\":0,\"message\":" + message
                + ",\"finish_reason\":\"" + reason + "\"}]}";
    }
}
