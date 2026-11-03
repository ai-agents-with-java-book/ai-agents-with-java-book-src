package ch15.agents;

import ch15.Models;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticSystemSuspendedException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.decision.DecisionModel;

import java.util.Map;

/**
 * Runs the workflows built around the decision agents of the chapter: the escalation gate,
 * the quality loop and the transaction gate. Requires OPENAI_API_KEY.
 */
public class DecisionAgentsMain {

    static final String URGENT_TICKET =
            "I was charged twice this month and my payroll runs tomorrow!";
    static final String QUESTION_TICKET = "How do I export last year's invoices as a CSV file?";
    static final String URGENCY_RULES = "any broken feature, or anything affecting invoices";

    public static void main(String[] args) {
        DecisionModel decisionModel = Models.decisionModel();
        ChatModel chatModel = Models.baseModel();

        // the gate chooses between a human handoff and an automatic reply
        UntypedAgent autoReply = DecisionAgentExamples.autoReply(decisionModel, chatModel);
        UntypedAgent supportWorkflow =
                DecisionAgentExamples.gateWorkflow(decisionModel, chatModel, autoReply);
        invoke("gate workflow", supportWorkflow,
                Map.of("ticket", URGENT_TICKET, "urgencyRules", URGENCY_RULES));
        invoke("gate workflow", supportWorkflow,
                Map.of("ticket", QUESTION_TICKET, "urgencyRules", URGENCY_RULES));

        // the grade of a decision agent is the exit condition of the loop
        UntypedAgent qualityLoop = DecisionAgentExamples.qualityLoop(decisionModel, chatModel);
        invoke("quality loop", qualityLoop,
                Map.of("ticket", QUESTION_TICKET, "reply", "Please check the documentation."));

        // the compliance approval is activated only when the gate says yes
        UntypedAgent transferWorkflow = DecisionAgentExamples.transferWorkflow(decisionModel);
        invoke("transfer workflow", transferWorkflow,
                Map.of("transfer", "Monthly rent, 1,200 EUR to Acme Properties, as every month"));
        invoke("transfer workflow", transferWorkflow,
                Map.of("transfer", "URGENT: 48,000 EUR to a new beneficiary abroad, today"));
    }

    static void invoke(String name, UntypedAgent workflow, Map<String, Object> input) {
        try {
            System.out.println(name + " -> " + workflow.invoke(input));
        } catch (AgenticSystemSuspendedException e) {
            System.out.println(name + " -> suspended, waiting for "
                    + e.scope().pendingResponseIds());
        }
    }
}
