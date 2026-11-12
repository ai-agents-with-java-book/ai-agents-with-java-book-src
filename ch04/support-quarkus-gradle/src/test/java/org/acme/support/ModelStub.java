package org.acme.support;

import java.util.Map;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class ModelStub implements QuarkusTestResourceLifecycleManager {
    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        return Map.of(
            "quarkus.langchain4j.openai.base-url", server.baseUrl() + "/v1",
            "quarkus.langchain4j.openai.api-key", "test-key",
            "quarkus.langchain4j.openai.chat-model.model-name", "test-model",
            "quarkus.langchain4j.openai.timeout", "2s",
            "quarkus.langchain4j.openai.max-retries", "1",
            "test.provider.port", Integer.toString(server.port()));
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
