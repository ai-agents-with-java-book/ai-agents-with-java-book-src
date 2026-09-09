package org.acme.repair;

import java.time.Duration;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static org.acme.repair.Repair.*;

public final class ModelPlanner implements Planner {
    private final RepairService service;

    public ModelPlanner(RepairService service) {
        this.service = service;
    }

    public static ModelPlanner openAi() {
        var model = OpenAiChatModel.builder()
                .apiKey(requiredEnvironment("OPENAI_API_KEY"))
                .modelName(requiredEnvironment("OPENAI_MODEL"))
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true)
                .maxRetries(0)
                .timeout(Duration.ofSeconds(30))
                .build();
        return new ModelPlanner(AiServices.create(RepairService.class, model));
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name);
        }
        return value;
    }

    @Override
    public Proposal propose(Snapshot snapshot) {
        var history = new StringBuilder();
        for (Attempt attempt : snapshot.history()) {
            history.append("Attempt ").append(attempt.number())
                    .append(" plan: ").append(attempt.proposal().plan())
                    .append("; passed: ").append(attempt.observation().passed())
                    .append('\n');
        }
        return service.propose("""
                Requirements:
                %s

                Previous attempts:
                %s

                Current source:
                %s

                Current verification status: %s
                Passing checks: %s
                Diagnostics:
                %s
                """.formatted(snapshot.goal(), history, snapshot.source(),
                    snapshot.observation().check(), snapshot.observation().passed(),
                    snapshot.observation().feedback()));
    }
}
