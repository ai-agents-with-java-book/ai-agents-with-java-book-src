package ch15;

import static org.assertj.core.api.Assertions.assertThat;

import ch15.agents.TransactionGate;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.response.DecisionAnswer;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.V;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransactionGateTest {

    public static class Ledger {
        @Agent(value = "Executes or holds a transfer", outputKey = "receipt")
        public String move(@V("transfer") String transfer, @V("suspicious") boolean suspicious) {
            return suspicious ? "held" : "executed";
        }
    }

    static final Map<String, Double> THRESHOLDS = Map.of("gpt-6-luna", 0.35);

    static Object process(DecisionModel decisionModel) {
        TransactionGate transactionGate = AgenticServices
                .decisionAgentBuilder(TransactionGate.class)
                .decisionModel(decisionModel)
                .thresholdProvider(context ->
                        THRESHOLDS.getOrDefault(context.modelName(), 0.5))
                .build();
        UntypedAgent transfers = AgenticServices.sequenceBuilder()
                .subAgents(transactionGate, new Ledger())
                .outputKey("receipt")
                .build();
        return transfers.invoke(
                Map.of("transfer", "12,000 EUR to a new beneficiary in another country"));
    }

    @Test
    void the_threshold_tuned_for_the_model_that_answered_is_applied() {
        Map<String, DecisionAnswer> answers = Map.of("check", YesNoAnswer.from(0.4));
        DecisionModel tunedModel = new StubDecisionModel("gpt-6-luna", answers);
        DecisionModel otherModel = new StubDecisionModel("jev-1.13.0", answers);

        assertThat(process(tunedModel)).isEqualTo("held");       // 0.4 >= 0.35
        assertThat(process(otherModel)).isEqualTo("executed");   // 0.4 < 0.5, the default
    }
}
