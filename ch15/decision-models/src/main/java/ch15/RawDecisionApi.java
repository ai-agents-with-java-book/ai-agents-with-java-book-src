package ch15;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.listener.DecisionModelListener;
import dev.langchain4j.model.decision.listener.DecisionModelResponseContext;
import dev.langchain4j.model.decision.request.ChoiceQuestion;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.request.DecisionRequestParameters;
import dev.langchain4j.model.decision.request.ScaleQuestion;
import dev.langchain4j.model.decision.request.YesNoQuestion;
import dev.langchain4j.model.decision.response.ChoiceAnswer;
import dev.langchain4j.model.decision.response.DecisionResponse;
import dev.langchain4j.model.decision.response.ScaleAnswer;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.model.openai.OpenAiDecisionModel;
import dev.langchain4j.model.openai.OpenAiDecisionModelName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RawDecisionApi {

    static void firstRequest(DecisionModel decisionModel) {
        DecisionRequest request = DecisionRequest.builder()
                .input("Help! My payouts have been failing for 3 days "
                        + "and nobody answers my emails.")
                .question("team", ChoiceQuestion.builder()
                        .text("Which team should handle this ticket?")
                        .option("billing", "Payments, payouts, invoices, refunds and charges")
                        .option("technical",
                                "Errors, outages, bugs and problems using the product")
                        .option("account", "Login, passwords, permissions and user management")
                        .option("other", "Anything else")
                        .build())
                .question("urgent",
                        YesNoQuestion.from("Does this ticket need attention today?"))
                .question("frustration", ScaleQuestion.builder()
                        .text("How frustrated is the customer?")
                        .level("calm", "A neutral or polite request")
                        .level("annoyed",
                                "The customer complains, or has already asked more than once")
                        .level("angry",
                                "The customer threatens to leave, or uses hostile language")
                        .build())
                .build();

        DecisionResponse response = decisionModel.decide(request);

        ChoiceAnswer team = response.choice("team");
        YesNoAnswer urgent = response.yesNo("urgent");
        ScaleAnswer frustration = response.scale("frustration");

        team.value();                   // "billing"
        team.probabilities();           // {billing=0.91, technical=0.06,
                                        //  account=0.02, other=0.01}
        team.margin();                  // 0.85, the gap between the two most likely options
        urgent.probability();           // 0.93, the probability of "yes"
        urgent.isYes(0.8);              // true
        frustration.mean();             // 1.4, between "annoyed" (1) and "angry" (2)
        frustration.probabilities();    // [0.05, 0.5, 0.45]
        response.modelName();           // "gpt-6-luna"
        response.tokenUsage();          // the tokens of this single call
    }

    static YesNoQuestion refundQuestion() {
        YesNoQuestion refundRequested = YesNoQuestion.builder()
                .text("Does the customer ask for money back?")
                .yesWhen("The customer explicitly asks for a refund, "
                        + "a chargeback or a credit note")
                .noWhen("The customer only asks what a charge is for, or how billing works")
                .build();
        return refundRequested;
    }

    static DecisionRequest fieldsRequest(String ticket, String plan, int openTickets) {
        return DecisionRequest.builder()
                .inputFields(Map.of(
                        "ticket", ticket,
                        "customer_plan", plan,
                        "open_tickets", openTickets,
                        "urgent_when", "any broken feature, or anything affecting invoices"))
                .question("urgent", YesNoQuestion.from(
                        "Does this ticket need attention today, given the urgency rules?"))
                .build();
    }

    static DecisionRequest imageRequest(String base64Photo) {
        return DecisionRequest.builder()
                .inputContents(List.of(
                        TextContent.from("The customer says the package arrived like this."),
                        ImageContent.from(base64Photo, "image/jpeg")))
                .question("damaged", YesNoQuestion.from("Is the item visibly damaged?"))
                .build();
    }

    static DecisionRequest conversationRequest(ChatMemory chatMemory) {
        return DecisionRequest.builder()
                .inputMessages(chatMemory.messages())
                .question("resolved", YesNoQuestion.from(
                        "Did the assistant fully answer the last question of the user?"))
                .question("handoff", YesNoQuestion.from(
                        "Did the user ask to talk to a human agent?"))
                .build();
    }

    static DecisionRequest pinnedModelRequest(String ticket) {
        return DecisionRequest.builder()
                .input(ticket)
                .question("urgent",
                        YesNoQuestion.from("Does this ticket need attention today?"))
                .parameters(DecisionRequestParameters.builder()
                        .modelName("gpt-6-luna")
                        .build())
                .build();
    }

    static void asyncCall(DecisionModel decisionModel, DecisionRequest request) {
        CompletableFuture<DecisionResponse> future = decisionModel.decideAsync(request);
        future.thenAccept(response -> {
            if (response.yesNo("urgent").isYes(0.8)) {
                notifyOnCallTeam(request);
            }
        });
    }

    static void refusals(DecisionResponse response, String ticket) {
        if (response.isRefused("urgent")) {
            escalateToHuman(ticket);
        } else if (response.yesNo("urgent").isYes(0.8)) {
            notifyOnCallTeam(ticket);
        }
    }

    static void failClosed(DecisionModel decisionModel, DecisionRequest request, String transfer) {
        try {
            if (decisionModel.decide(request).yesNo("suspicious").isYes(0.5)) {
                hold(transfer);
            } else {
                execute(transfer);
            }
        } catch (LangChain4jException e) {
            // authentication, rate limit, timeout, invalid response: a gate fails closed
            hold(transfer);
        }
    }

    static DecisionModel auditedDecisionModel(AuditLog auditLog) {
        DecisionModel decisionModel = OpenAiDecisionModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(OpenAiDecisionModelName.GPT_6_LUNA)
                .listeners(new DecisionModelListener() {

                    @Override
                    public void onResponse(DecisionModelResponseContext context) {
                        // the toString of the request leaves the input out
                        auditLog.record(
                                context.decisionRequest().toString(),
                                context.decisionResponse().answers(),
                                context.decisionResponse().modelName(),
                                context.decisionResponse().tokenUsage());
                    }
                })
                .build();
        return decisionModel;
    }

    interface AuditLog {
        void record(String request, Object answers, String modelName, Object tokenUsage);
    }

    static void notifyOnCallTeam(Object ticket) {}

    static void escalateToHuman(Object ticket) {}

    static void hold(String transfer) {}

    static void execute(String transfer) {}

    static void sample() {
        UserMessage.from("x");
        AiMessage.from("y");
    }
}
