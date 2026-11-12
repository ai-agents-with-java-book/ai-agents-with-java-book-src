package org.acme.support;

import java.time.Duration;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

public class SummaryCli {
    public static SupportSummary createService() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String modelName = System.getenv("OPENAI_MODEL");
        if (apiKey == null || modelName == null) {
            throw new IllegalStateException(
                "Set OPENAI_API_KEY and OPENAI_MODEL before running");
        }
        var model = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .baseUrl(System.getenv().getOrDefault(
                "OPENAI_BASE_URL", "https://api.openai.com/v1"))
            .timeout(Duration.ofSeconds(20))
            .maxRetries(0)
            .build();
        return AiServices.builder(SupportSummary.class)
            .chatModel(model)
            .build();
    }

    public static void main(String[] args) {
        var drafts = new SupportDrafts(createService());
        System.out.println(drafts.prepareSummary(String.join(" ", args)));
    }
}
