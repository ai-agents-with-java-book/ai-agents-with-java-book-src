package org.aiagents.observability;

import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class TokenMetricsRecorder {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public TokenMetricsRecorder(
            MeterRegistry registry) {
        this.registry = registry;
    }

    private Counter getOrCreateCounter(String name) {
        return counters.computeIfAbsent(name, k ->
                        Counter.builder(name)
                                .tag("appName", "expert-chatbot")
                                .description("Total number of " + name.replace("llm_token_", "").replace("_tokens_total", ""))
                .register(registry)
        );
    }

    public void onAiServiceResponseReceived(@Observes AiServiceResponseReceivedEvent event) {
        var response = event.response();
        if (response == null || response.tokenUsage() == null) {
            return;
        }

        var usage = response.tokenUsage();

        getOrCreateCounter("llm_token_input_count_tokens_total")
                .increment(usage.inputTokenCount());

        getOrCreateCounter("llm_token_output_count_tokens_total")
                .increment(usage.outputTokenCount());

        getOrCreateCounter("llm_token_count_tokens_total")
                .increment(usage.totalTokenCount());
    }
}