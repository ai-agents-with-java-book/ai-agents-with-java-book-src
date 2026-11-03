package ch15;

import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.response.DecisionResponse;
import dev.langchain4j.service.decision.Choice;
import dev.langchain4j.service.decision.DecisionResult;
import dev.langchain4j.service.decision.DecisionServices;
import dev.langchain4j.service.decision.Scale;

/**
 * Asks the decision model the questions of the chapter, first through the raw API
 * and then through the {@link SupportDesk} decision service. Requires OPENAI_API_KEY.
 */
public class SupportDeskMain {

    static final String TICKET = "I was charged twice this month and my payroll runs tomorrow!";
    static final String URGENCY_RULES = "any broken feature, or anything affecting invoices";

    public static void main(String[] args) {
        DecisionModel decisionModel = Models.decisionModel();

        // the raw API: a request with input fields and a yes/no question
        DecisionRequest request = RawDecisionApi.fieldsRequest(TICKET, "enterprise", 3);
        DecisionResponse response = decisionModel.decide(request);
        System.out.println("urgent: " + response.yesNo("urgent").probability()
                + " (" + response.modelName() + ", " + response.tokenUsage() + ")");

        // the same kind of questions through a decision service
        SupportDesk supportDesk = DecisionServices.create(SupportDesk.class, decisionModel);

        System.out.println("spam: " + supportDesk.isSpam(TICKET));
        System.out.println("spam: " + supportDesk.isSpam("Congratulations! You won a free cruise!"));

        Choice<Team> team = supportDesk.routeWithProbabilities(TICKET);
        System.out.println("team: " + team.value() + " " + team.probabilities()
                + " margin " + team.margin());

        Scale<Frustration> frustration = supportDesk.frustration(TICKET);
        System.out.println("frustration: " + frustration.mostLikely()
                + " mean " + frustration.mean() + " " + frustration.probabilities());

        SupportDesk.Triage triage = supportDesk.triage(TICKET, URGENCY_RULES);
        System.out.println("triage: " + triage);

        DecisionResult<Team> result = supportDesk.routeWithMetadata(TICKET);
        System.out.println("routed by " + result.modelName() + " using " + result.tokenUsage());
    }
}
