package ch15.router;

import ch15.Models;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.decision.DecisionModel;

/**
 * Routes support tickets to the expert agents with the DecisionRouterPlanner, to a single
 * team and then to every team concerned. Requires OPENAI_API_KEY.
 */
public class RouterMain {

    public static void main(String[] args) {
        DecisionModel decisionModel = Models.decisionModel();
        ChatModel chatModel = Models.baseModel();

        // one team per ticket: the router answers a billing ticket while it is built
        SupportRouter supportRouter = RouterExamples.supportRouter(decisionModel, chatModel);
        System.out.println(supportRouter.answer(
                "Since the last update the export button does nothing."));

        // every team whose probability reaches the threshold, then a single reply
        BillingExpert billingExpert = AgenticServices.agentBuilder(BillingExpert.class)
                .chatModel(chatModel)
                .build();
        TechnicalExpert technicalExpert = AgenticServices.agentBuilder(TechnicalExpert.class)
                .chatModel(chatModel)
                .build();
        AccountExpert accountExpert = AgenticServices.agentBuilder(AccountExpert.class)
                .chatModel(chatModel)
                .build();
        SupportPipeline supportPipeline = RouterExamples.supportPipeline(
                decisionModel, chatModel, billingExpert, technicalExpert, accountExpert);
        System.out.println(supportPipeline.reply(
                "I cannot log in since yesterday, and I need a refund for the downtime."));
    }
}
