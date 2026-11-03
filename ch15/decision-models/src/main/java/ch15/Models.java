package ch15;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.ollama.OllamaDecisionModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiDecisionModel;
import dev.langchain4j.model.openai.OpenAiDecisionModelName;
import dev.langchain4j.model.typesafe.TypeSafeDecisionModel;

import java.time.Duration;

public class Models {

    public static DecisionModel decisionModel() {
        return OpenAiDecisionModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(OpenAiDecisionModelName.GPT_6_LUNA)
                .build();
    }

    public static DecisionModel productionDecisionModel() {
        return OpenAiDecisionModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(OpenAiDecisionModelName.GPT_6_LUNA)
                .timeout(Duration.ofSeconds(5))
                .maxRetries(1)
                .build();
    }

    public static DecisionModel typeSafeDecisionModel() {
        return TypeSafeDecisionModel.builder()
                .apiKey(System.getenv("TYPESAFE_API_KEY"))
                .modelName("jev-1.13.0")
                .build();
    }

    public static DecisionModel ollamaDecisionModel() {
        return OllamaDecisionModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nimble")
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    public static ChatModel baseModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();
    }

    public static ChatModel largeModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_1)
                .build();
    }
}
