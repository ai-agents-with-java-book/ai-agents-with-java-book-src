package ch15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.response.ChoiceAnswer;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.decision.Choice;
import dev.langchain4j.service.decision.DecisionServices;
import org.junit.jupiter.api.Test;

class SupportDeskTest {

    @Test
    void a_small_margin_reveals_a_hesitation_between_two_teams() {
        DecisionModel decisionModel = StubDecisionModel.answering("routeWithProbabilities",
                ChoiceAnswer.builder()
                        .value("BILLING")
                        .probability("BILLING", 0.52)
                        .probability("TECHNICAL", 0.41)
                        .probability("ACCOUNT", 0.05)
                        .probability("SALES", 0.02)
                        .build());
        SupportDesk supportDesk = DecisionServices.create(SupportDesk.class, decisionModel);

        Choice<Team> choice = supportDesk.routeWithProbabilities(
                "I was charged for a month I could not log in");

        assertThat(choice.value()).isEqualTo(Team.BILLING);
        assertThat(choice.margin()).isCloseTo(0.11, within(0.001));
    }

    @Test
    void a_boolean_answer_applies_the_threshold_of_the_service() {
        DecisionModel decisionModel = StubDecisionModel.answering("isSpam", YesNoAnswer.from(0.8));
        SupportDesk strict = DecisionServices.builder(SupportDesk.class)
                .decisionModel(decisionModel)
                .thresholdProvider(context -> 0.9)
                .build();
        SupportDesk lenient = DecisionServices.create(SupportDesk.class, decisionModel);

        assertThat(strict.isSpam("Buy now!")).isFalse();
        assertThat(lenient.isSpam("Buy now!")).isTrue();
    }
}
