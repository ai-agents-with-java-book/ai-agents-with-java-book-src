package ch15;

import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.response.DecisionAnswer;
import dev.langchain4j.model.decision.response.DecisionResponse;

import java.util.Map;

public class StubDecisionModel implements DecisionModel {

    private final String modelName;
    private final Map<String, DecisionAnswer> answers;

    public StubDecisionModel(String modelName, Map<String, DecisionAnswer> answers) {
        this.modelName = modelName;
        this.answers = answers;
    }

    public static StubDecisionModel answering(String question, DecisionAnswer answer) {
        return new StubDecisionModel("stub", Map.of(question, answer));
    }

    @Override
    public DecisionResponse doDecide(DecisionRequest request) {
        return DecisionResponse.builder()
                .answers(answers)          // checked against the questions
                .modelName(modelName)
                .build();
    }
}
