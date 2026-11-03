package ch15.declarative;

import ch15.Models;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.DecisionModelSupplier;
import dev.langchain4j.agentic.declarative.DecisionThresholdSupplier;
import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;
import dev.langchain4j.service.decision.ThresholdContext;

public interface TransactionGate {

    @Decide("""
            Does this money transfer look suspicious?
            Yes: an unusually large amount, an unknown or foreign beneficiary,
            an urgent or unusual justification.
            No: a routine payment to a known beneficiary.""")
    @Agent(value = "Checks whether a money transfer looks suspicious",
            outputKey = "suspicious")
    boolean check(@V("transfer") String transfer);

    @DecisionModelSupplier
    static DecisionModel decisionModel() {
        return Models.decisionModel();
    }

    @DecisionThresholdSupplier
    static double threshold(ThresholdContext context) {
        // tuned per model, read at every invocation from a property
        // like suspicious.threshold.gpt-6-luna
        String property = "suspicious.threshold." + context.modelName();
        return Double.parseDouble(System.getProperty(property, "0.5"));
    }
}
