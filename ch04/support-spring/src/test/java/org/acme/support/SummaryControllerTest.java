package org.acme.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SummaryControllerTest {
    static final WireMockServer provider = new WireMockServer(options().dynamicPort());
    static { provider.start(); }
    @Autowired TestRestTemplate http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry config) {
        config.add("langchain4j.open-ai.chat-model.base-url", () -> provider.baseUrl() + "/v1");
        config.add("langchain4j.open-ai.chat-model.api-key", () -> "test-key");
        config.add("langchain4j.open-ai.chat-model.model-name", () -> "test-model");
        config.add("langchain4j.open-ai.chat-model.timeout", () -> "2s");
        config.add("langchain4j.open-ai.chat-model.max-retries", () -> "0");
    }

    static final String RESPONSE = """
        {
          "id": "chatcmpl-test", "object": "chat.completion",
          "created": 0, "model": "test-model",
          "choices": [{"index": 0, "finish_reason": "stop",
            "message": {"role": "assistant",
              "content": "ORD-1042 is late. Tracking is missing."}}],
          "usage": {"prompt_tokens": 20, "completion_tokens": 10,
            "total_tokens": 30}
        }
        """;

    @BeforeEach
    void configureProvider() {
        configureFor("localhost", provider.port());
        reset();
        stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(RESPONSE)));
    }

    @AfterAll
    static void stopProvider() { provider.stop(); }

    private org.springframework.http.ResponseEntity<String> summarize(String text) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return http.postForEntity("/summaries", new HttpEntity<>(text, headers), String.class);
    }

    @Test
    void returnsTheProviderResult() {
        var result = summarize("Where is ORD-1042?");
        assertEquals(200, result.getStatusCode().value());
        assertEquals("ORD-1042 is late. Tracking is missing.", result.getBody());
        verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions"))
            .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
            .withRequestBody(containing("ORD-1042")));
    }

    @Test
    void rejectsBlankInputWithoutCallingTheProvider() {
        var result = summarize("  ");
        assertEquals(400, result.getStatusCode().value());
        assertEquals("Support request is required", result.getBody());
        verify(0, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    @Test
    void reportsAProviderTimeout() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(okJson(RESPONSE).withFixedDelay(4000)));
        var result = summarize("Where is ORD-1042?");
        assertEquals(502, result.getStatusCode().value());
        assertEquals("Summary provider unavailable", result.getBody());
        verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }
}
