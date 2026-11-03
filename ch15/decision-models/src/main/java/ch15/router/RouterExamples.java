package ch15.router;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.decisionrouter.DecisionRouterPlanner;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.decision.DecisionModel;

import java.util.Map;

public class RouterExamples {

    static SupportRouter supportRouter(DecisionModel decisionModel, ChatModel chatModel) {
        BillingExpert billingExpert = AgenticServices.agentBuilder(BillingExpert.class)
                .chatModel(chatModel)
                .build();
        TechnicalExpert technicalExpert = AgenticServices.agentBuilder(TechnicalExpert.class)
                .chatModel(chatModel)
                .build();
        AccountExpert accountExpert = AgenticServices.agentBuilder(AccountExpert.class)
                .chatModel(chatModel)
                .build();

        SupportRouter supportRouter = AgenticServices.plannerBuilder(SupportRouter.class)
                .subAgents(billingExpert, technicalExpert, accountExpert)
                .outputKey("answer")
                .planner(() -> new DecisionRouterPlanner(decisionModel))
                .build();

        String answer = supportRouter.answer(
                "I was charged twice this month, can you refund one charge?");
        System.out.println(answer);
        return supportRouter;
    }

    static SupportPipeline supportPipeline(DecisionModel decisionModel, ChatModel chatModel,
                                           BillingExpert billingExpert, TechnicalExpert technicalExpert,
                                           AccountExpert accountExpert) {
        MultiSupportRouter multiSupportRouter = AgenticServices
                .plannerBuilder(MultiSupportRouter.class)
                .subAgents(billingExpert, technicalExpert, accountExpert)
                .outputKey("answers")
                .planner(() -> new DecisionRouterPlanner(decisionModel, 0.5))
                .build();

        Map<String, String> answers = multiSupportRouter.answer(
                "Since the last update I cannot log in, "
                        + "and I was still charged for the month I could not use!");
        // {technical=..., account=..., billing=...}:
        // every team whose probability reached 0.5, invoked in parallel
        System.out.println(answers);

        AnswerSynthesizer answerSynthesizer = AgenticServices.agentBuilder(AnswerSynthesizer.class)
                .chatModel(chatModel)
                .build();

        return AgenticServices.sequenceBuilder(SupportPipeline.class)
                .subAgents(multiSupportRouter, answerSynthesizer)
                .outputKey("reply")
                .build();
    }

    static HiringRouter hiringRouter(DecisionModel decisionModel, ChatModel chatModel) {
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(chatModel)
                .build();
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(chatModel)
                .build();

        return AgenticServices.plannerBuilder(HiringRouter.class)
                .subAgents(interviewOrganizer, emailAssistant)
                .outputKey("outcome")
                .planner(() -> new DecisionRouterPlanner(decisionModel))
                .build();
    }
}
