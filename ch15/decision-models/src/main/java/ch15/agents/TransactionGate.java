package ch15.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;

public interface TransactionGate {

    @Decide("""
            Does this money transfer look suspicious?
            Yes: an unusually large amount, an unknown or foreign beneficiary,
            an urgent or unusual justification.
            No: a routine payment to a known beneficiary.""")
    @Agent(value = "Checks whether a money transfer looks suspicious",
            outputKey = "suspicious")
    boolean check(@V("transfer") String transfer);
}
