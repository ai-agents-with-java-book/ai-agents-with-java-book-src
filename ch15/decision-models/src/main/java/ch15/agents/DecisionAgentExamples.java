package ch15.agents;

import ch15.Models;
import ch15.Quality;
import ch15.Team;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.SuspendedResponse;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.service.decision.Scale;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DecisionAgentExamples {

    static UntypedAgent gateWorkflow(DecisionModel decisionModel, ChatModel chatModel, UntypedAgent autoReply) {
        EscalationGate escalationGate = AgenticServices
                .decisionAgentBuilder(EscalationGate.class)
                .decisionModel(decisionModel)
                .threshold(0.6)
                .build();

        HumanInTheLoop handoff = AgenticServices.humanInTheLoopBuilder()
                .description("Hands the ticket over to a human support agent")
                .outputKey("reply")
                .responseProvider(scope -> new SuspendedResponse<>("human-support"))
                .build();

        UntypedAgent ticketRouter = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("needsHuman", false), handoff)
                .subAgents(scope -> !scope.readState("needsHuman", false), autoReply)
                .build();

        return AgenticServices.sequenceBuilder()
                .subAgents(escalationGate, ticketRouter)
                .outputKey("reply")
                .build();
    }

    static void invokeGateWorkflow(UntypedAgent supportWorkflow) {
        Object reply = supportWorkflow.invoke(Map.of(
                "ticket", "I was charged twice this month and my payroll runs tomorrow!",
                "urgencyRules", "any broken feature, or anything affecting invoices"));
    }

    static UntypedAgent autoReply(DecisionModel decisionModel, ChatModel chatModel) {
        TicketTriage ticketTriage = AgenticServices.decisionAgentBuilder(TicketTriage.class)
                .decisionModel(decisionModel)
                .build();

        AgenticServices.AgenticScopeAction spreadTriage = AgenticServices.agentAction(scope -> {
            TicketTriage.Triage triage = (TicketTriage.Triage) scope.readState("triage");
            scope.writeState("team", triage.team());
            scope.writeState("refund", triage.refund());
        });

        SupportAssistant supportAssistant = AgenticServices.agentBuilder(SupportAssistant.class)
                .chatModel(chatModel)
                .build();

        return AgenticServices.sequenceBuilder()
                .subAgents(ticketTriage, spreadTriage, supportAssistant)
                .outputKey("reply")
                .build();
    }

    static UntypedAgent qualityLoop(DecisionModel decisionModel, ChatModel chatModel) {
        ReplyGrader replyGrader = AgenticServices.decisionAgentBuilder(ReplyGrader.class)
                .decisionModel(decisionModel)
                .build();

        ReplyEditor replyEditor = AgenticServices.agentBuilder(ReplyEditor.class)
                .chatModel(chatModel)
                .build();

        return AgenticServices.loopBuilder()
                .subAgents(replyGrader, replyEditor)
                .maxIterations(3)
                .exitCondition(scope -> {
                    Scale<Quality> quality = (Scale<Quality>) scope.readState("replyQuality");
                    return quality.mean() >= 2;      // on average at least GOOD
                })
                .outputKey("reply")
                .build();
    }

    static UntypedAgent transferWorkflow(DecisionModel decisionModel) {
        Map<String, Double> thresholds = new ConcurrentHashMap<>(Map.of("gpt-6-luna", 0.35));

        TransactionGate transactionGate = AgenticServices
                .decisionAgentBuilder(TransactionGate.class)
                .decisionModel(decisionModel)
                .thresholdProvider(context -> thresholds.getOrDefault(context.modelName(), 0.5))
                .build();

        HumanInTheLoop complianceApproval = AgenticServices.humanInTheLoopBuilder()
                .description("Compliance approval of a suspicious transfer")
                .outputKey("approval")
                .responseProvider(scope -> new SuspendedResponse<>("compliance-approval"))
                .activationCondition("the transfer looks suspicious",
                        scope -> scope.readState("suspicious", false))
                .build();

        return AgenticServices.sequenceBuilder()
                .subAgents(transactionGate, complianceApproval, new MoneyMover())
                .outputKey("receipt")
                .build();
    }

    static TicketTriage auditedTriage(DecisionModel decisionModel) {
        return AgenticServices.decisionAgentBuilder(TicketTriage.class)
                .decisionModel(decisionModel)
                .listener(new DecisionAuditListener())
                .build();
    }

    static TicketTriage triagePerCustomer(DecisionModel standardModel, DecisionModel premiumModel) {
        return AgenticServices.decisionAgentBuilder(TicketTriage.class)
                .decisionModelProvider(scope ->
                        "enterprise".equals(scope.readState("plan")) ? premiumModel : standardModel)
                .build();
    }

    static UntypedAgent writeupLoop(DecisionModel decisionModel, Object writer) {
        WriteupScorer scorer = AgenticServices.decisionAgentBuilder(WriteupScorer.class)
                .decisionModel(decisionModel)
                .build();

        return AgenticServices.loopBuilder()
                .subAgents(writer, scorer)
                .outputKey("writeup")
                .exitCondition(scope ->
                        ((Scale<WriteupScorer.Alignment>) scope.readState("score")).mean() >= 2)
                .maxIterations(5)
                .build();
    }

    static void unused(AgenticScope scope, Team team) {
        Models.baseModel();
    }
}
