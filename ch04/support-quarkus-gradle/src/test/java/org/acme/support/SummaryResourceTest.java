package org.acme.support;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(ModelStub.class)
class SummaryResourceTest {
    @ConfigProperty(name = "test.provider.port")
    int providerPort;

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
        configureFor("localhost", providerPort);
        reset();
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(okJson(RESPONSE)));
    }

    @Test
    void returnsTheProviderResult() {
        given().contentType("text/plain").body("Where is ORD-1042?")
            .when().post("/summaries")
            .then().statusCode(200)
            .body(equalTo("ORD-1042 is late. Tracking is missing."));
        verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions"))
            .withRequestBody(matchingJsonPath("$.model",
                com.github.tomakehurst.wiremock.client.WireMock.equalTo("test-model")))
            .withRequestBody(containing("ORD-1042")));
    }

    @Test
    void rejectsBlankInputWithoutCallingTheProvider() {
        given().contentType("text/plain").body("  ")
            .when().post("/summaries")
            .then().statusCode(400)
            .body(equalTo("Support request is required"));
        verify(0, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    @Test
    void reportsAProviderTimeout() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(okJson(RESPONSE).withFixedDelay(4000)));
        given().contentType("text/plain").body("Where is ORD-1042?")
            .when().post("/summaries")
            .then().statusCode(502)
            .body(equalTo("Summary provider unavailable"));
        verify(2, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }
}
