package ch15;

import static org.assertj.core.api.Assertions.assertThat;

import ch15.agents.ReplyGrader;
import ch15.agents.TicketTriage;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.response.ChoiceAnswer;
import dev.langchain4j.model.decision.response.ScaleAnswer;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Scale;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TriageAndLoopVerificationTest {

    public static class Replier {
        @Agent(value = "Replies", outputKey = "reply")
        public String reply(@V("ticket") String ticket, @V("team") Team team, @V("refund") boolean refund) {
            return team + "/" + refund;
        }
    }

    @Test
    void a_record_answer_is_spread_and_consumed_downstream() {
        DecisionModel decisionModel = new StubDecisionModel("stub", Map.of(
                "team", ChoiceAnswer.builder().value("BILLING").probability("BILLING", 0.9).build(),
                "refund", YesNoAnswer.from(0.97)));
        TicketTriage ticketTriage = AgenticServices.decisionAgentBuilder(TicketTriage.class)
                .decisionModel(decisionModel)
                .build();
        AgenticServices.AgenticScopeAction spreadTriage = AgenticServices.agentAction(scope -> {
            TicketTriage.Triage triage = (TicketTriage.Triage) scope.readState("triage");
            scope.writeState("team", triage.team());
            scope.writeState("refund", triage.refund());
        });
        UntypedAgent workflow = AgenticServices.sequenceBuilder()
                .subAgents(ticketTriage, spreadTriage, new Replier())
                .outputKey("reply")
                .build();

        assertThat(workflow.invoke(Map.of("ticket", "charged twice, refund please"))).isEqualTo("BILLING/true");
    }

    public static class Editor {
        static final AtomicInteger EDITS = new AtomicInteger();
        @Agent(value = "Edits", outputKey = "reply")
        public String edit(@V("ticket") String ticket, @V("reply") String reply) {
            EDITS.incrementAndGet();
            return reply + "+";
        }
    }

    @Test
    void the_loop_stops_when_the_mean_quality_is_at_least_good() {
        DecisionModel decisionModel = StubDecisionModel.answering("grade",
                ScaleAnswer.builder().mean(2.4).probabilities(List.of(0.0, 0.1, 0.4, 0.5)).build());
        ReplyGrader replyGrader = AgenticServices.decisionAgentBuilder(ReplyGrader.class)
                .decisionModel(decisionModel)
                .build();
        UntypedAgent loop = AgenticServices.loopBuilder()
                .subAgents(replyGrader, new Editor())
                .maxIterations(3)
                .exitCondition(scope -> {
                    Scale<Quality> quality = (Scale<Quality>) scope.readState("replyQuality");
                    return quality.mean() >= 2;
                })
                .outputKey("reply")
                .build();

        Object reply = loop.invoke(Map.of("ticket", "t", "reply", "draft"));

        assertThat(reply).isEqualTo("draft");
        assertThat(Editor.EDITS.get()).isZero();
    }
}
