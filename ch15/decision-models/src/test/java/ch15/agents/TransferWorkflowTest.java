package ch15.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch15.StubDecisionModel;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticSystemSuspendedException;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransferWorkflowTest {

    static final Map<String, Object> TRANSFER = Map.of("transfer", "1,200 EUR to Acme Properties");

    @Test
    void a_routine_transfer_skips_the_approval_and_is_executed() {
        UntypedAgent transferWorkflow = DecisionAgentExamples.transferWorkflow(
                new StubDecisionModel("gpt-6-luna", Map.of("check", YesNoAnswer.from(0.1))));

        assertThat(transferWorkflow.invoke(TRANSFER))
                .isEqualTo("Transfer executed: 1,200 EUR to Acme Properties");
    }

    @Test
    void a_suspicious_transfer_waits_for_the_compliance_approval() {
        UntypedAgent transferWorkflow = DecisionAgentExamples.transferWorkflow(
                new StubDecisionModel("gpt-6-luna", Map.of("check", YesNoAnswer.from(0.9))));

        assertThatThrownBy(() -> transferWorkflow.invoke(TRANSFER))
                .isInstanceOfSatisfying(AgenticSystemSuspendedException.class, suspended -> {
                    assertThat(suspended.scope().readState("suspicious")).isEqualTo(true);
                    assertThat(suspended.scope().pendingResponseIds())
                            .containsExactly("compliance-approval");
                });
    }
}
