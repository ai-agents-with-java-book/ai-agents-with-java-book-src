package ch15.cascade;

import ch15.Models;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.internal.SuspendedResponse;
import dev.langchain4j.agentic.patterns.cascade.ConfidenceCascadePlanner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.decision.DecisionModel;

public class CascadeExamples {

    static SupportCascade supportCascade(DecisionModel decisionModel) {
        FaqAgent faqAgent = AgenticServices.agentBuilder(FaqAgent.class)
                .chatModel(Models.baseModel())
                .build();
        ExpertAgent expertAgent = AgenticServices.agentBuilder(ExpertAgent.class)
                .chatModel(Models.largeModel())
                .build();
        HumanInTheLoop humanSupport = AgenticServices.humanInTheLoopBuilder()
                .description("A human support engineer answering the request")
                .outputKey("humanAnswer")
                .responseProvider(scope -> new SuspendedResponse<>("human-support"))
                .build();

        return AgenticServices.plannerBuilder(SupportCascade.class)
                .subAgents(faqAgent, expertAgent, humanSupport)
                .outputKey("answer")
                .planner(() -> new ConfidenceCascadePlanner(decisionModel, 0.7,
                        "The answer gives concrete steps that solve the customer's request, "
                                + "instead of saying that it cannot help"))
                .build();
    }

    static void ask(SupportCascade supportCascade) {
        ResultWithAgenticScope<String> result =
                supportCascade.ask("ticket-4711",
                        "How do I export last year's invoices as a CSV file?");

        if (result.suspended()) {
            // both models were rejected by the grade:
            // the request is in the queue of the human engineers
            String humanAnswer = waitForHumanAnswer("ticket-4711");
            result = result.completePendingResponse(humanAnswer);
        }
        String answer = result.result();
        System.out.println(answer);
    }

    static String waitForHumanAnswer(String ticketId) {
        // stands for the queue of the human engineers
        return "Open Billing > Invoices, select last year and click Export as CSV.";
    }
}
