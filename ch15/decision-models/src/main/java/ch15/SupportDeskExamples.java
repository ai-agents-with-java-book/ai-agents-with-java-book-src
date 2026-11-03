package ch15;

import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.request.DecisionRequestParameters;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.decision.Choice;
import dev.langchain4j.service.decision.DecisionResult;
import dev.langchain4j.service.decision.DecisionServices;
import dev.langchain4j.service.decision.Scale;

import java.util.List;
import java.util.Map;

public class SupportDeskExamples {

    static void basics(DecisionModel decisionModel) {
        SupportDesk supportDesk = DecisionServices.create(SupportDesk.class, decisionModel);

        boolean spam = supportDesk.isSpam("Congratulations! You won a free cruise!"); // true
        Team team = supportDesk.route("I was charged twice this month");              // BILLING
    }

    static void probabilities(SupportDesk supportDesk, String ticket, HumanQueue humanQueue) {
        Choice<Team> choice = supportDesk.routeWithProbabilities(ticket);
        choice.value();                      // BILLING
        choice.probabilities();              // {BILLING=0.52, TECHNICAL=0.41,
                                             //  ACCOUNT=0.05, SALES=0.02}
        choice.probabilityOf(Team.TECHNICAL);
        if (choice.margin() < 0.2) {         // the model hesitated between two teams
            humanQueue.add(ticket);
        } else {
            assign(ticket, choice.value());
        }
    }

    static void scale(SupportDesk supportDesk, String ticket) {
        Scale<Frustration> frustration = supportDesk.frustration(ticket);
        frustration.mostLikely();                             // ANNOYED
        frustration.mean();                                   // 1.4, from 0 (CALM) to 2 (ANGRY)
        frustration.probabilityAtLeast(Frustration.ANNOYED);  // 0.95
        if (frustration.probabilityOf(Frustration.ANGRY) > 0.4) {
            notifyAccountManager(ticket);
        }
    }

    static void triage(SupportDesk supportDesk, String ticket, Customer customer) {
        SupportDesk.Triage triage = supportDesk.triage(ticket, customer.plan().urgencyRules());
        triage.team();                       // BILLING
        triage.urgent();                     // true
        triage.refund().probability();       // 0.97
    }

    static void metadata(SupportDesk supportDesk, String ticket) {
        DecisionResult<Team> result = supportDesk.routeWithMetadata(ticket);
        result.content();        // BILLING
        result.modelName();      // "gpt-6-luna"
        result.tokenUsage();
        result.response();       // the DecisionResponse, answer under "routeWithMetadata"
    }

    static void pinnedModel(SupportDesk supportDesk, String ticket) {
        Team team = supportDesk.route(ticket, DecisionRequestParameters.builder()
                .modelName("gpt-6-luna")
                .build());
    }

    static SupportDesk withThresholds(DecisionModel decisionModel, Config config) {
        SupportDesk supportDesk = DecisionServices.builder(SupportDesk.class)
                .decisionModel(decisionModel)
                .thresholdProvider(context -> config.getDouble(
                        // e.g. SupportDesk.isSpam.gpt-6-luna=0.9
                        context.serviceInterface().getSimpleName()
                                + "." + context.questionName()
                                + "." + context.modelName()))
                .build();
        return supportDesk;
    }

    record Example(String text, boolean spam) {}

    static void evaluateThresholds(SupportDesk supportDesk, List<Example> dataset) {
        List<YesNoAnswer> answers = dataset.stream()
                .map(example -> supportDesk.spamProbability(example.text()))
                .toList();                                       // one call per example

        for (double threshold = 0.5; threshold < 1; threshold += 0.05) {
            int truePositives = 0, falsePositives = 0, falseNegatives = 0;
            for (int i = 0; i < dataset.size(); i++) {
                boolean predicted = answers.get(i).isYes(threshold);   // no further call
                boolean actual = dataset.get(i).spam();
                if (predicted && actual) truePositives++;
                if (predicted && !actual) falsePositives++;
                if (!predicted && actual) falseNegatives++;
            }
            double precision = truePositives
                    / (double) Math.max(1, truePositives + falsePositives);
            double recall = truePositives
                    / (double) Math.max(1, truePositives + falseNegatives);
            System.out.printf("threshold %.2f: precision %.2f, recall %.2f%n",
                    threshold, precision, recall);
        }
    }

    interface HumanQueue {
        void add(String ticket);
    }

    interface Config {
        Double getDouble(String key);
    }

    record Plan(String urgencyRules) {}

    record Customer(Plan plan) {}

    static void assign(String ticket, Team team) {}

    static void notifyAccountManager(String ticket) {}

    static Map<String, Object> unused() {
        return Map.of();
    }
}
