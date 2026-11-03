package ch15;

import static org.assertj.core.api.Assertions.assertThat;

import ch15.agents.EscalationGate;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.V;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EscalationGateTest {

    public static class Handoff {
        @Agent(value = "Hands the ticket over to a human support agent", outputKey = "reply")
        public String handoff(@V("ticket") String ticket) {
            return "A support agent will contact you about: " + ticket;
        }
    }

    public static class AutoReply {
        @Agent(value = "Answers the ticket from the knowledge base", outputKey = "reply")
        public String reply(@V("ticket") String ticket) {
            return "Our knowledge base answers: " + ticket;
        }
    }

    static UntypedAgent supportWorkflow(DecisionModel decisionModel, double threshold) {
        EscalationGate escalationGate = AgenticServices
                .decisionAgentBuilder(EscalationGate.class)
                .decisionModel(decisionModel)
                .threshold(threshold)
                .build();
        UntypedAgent ticketRouter = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("needsHuman", false), new Handoff())
                .subAgents(scope -> !scope.readState("needsHuman", false), new AutoReply())
                .build();
        return AgenticServices.sequenceBuilder()
                .subAgents(escalationGate, ticketRouter)
                .outputKey("reply")
                .build();
    }

    static final Map<String, Object> TICKET = Map.of(
            "ticket", "I cannot log in since yesterday and my invoices are due today",
            "urgencyRules", "anything affecting invoices");

    @Test
    void hands_over_when_the_probability_reaches_the_threshold() {
        DecisionModel decisionModel =
                StubDecisionModel.answering("check", YesNoAnswer.from(0.62));

        Object reply = supportWorkflow(decisionModel, 0.6).invoke(TICKET);

        assertThat(reply).asString().startsWith("A support agent will contact you");
    }

    @Test
    void answers_automatically_below_the_threshold() {
        DecisionModel decisionModel =
                StubDecisionModel.answering("check", YesNoAnswer.from(0.62));

        Object reply = supportWorkflow(decisionModel, 0.7).invoke(TICKET);

        assertThat(reply).asString().startsWith("Our knowledge base answers");
    }
}
