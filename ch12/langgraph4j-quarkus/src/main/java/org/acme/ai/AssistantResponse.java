package org.acme.ai;

import dev.langchain4j.model.output.structured.Description;

public record AssistantResponse(
    String response,
    @Description("Set true if the model requires more information") boolean isResponseAQuestion) {
}
