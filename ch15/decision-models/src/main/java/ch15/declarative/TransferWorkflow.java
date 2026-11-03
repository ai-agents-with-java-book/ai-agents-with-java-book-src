package ch15.declarative;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

public interface TransferWorkflow {

    @SequenceAgent(outputKey = "receipt",
            subAgents = {TransactionGate.class, MoneyMover.class})
    String process(@V("transfer") String transfer);

    static String run(String transfer) {
        TransferWorkflow transferWorkflow =
                AgenticServices.createAgenticSystem(TransferWorkflow.class);
        return transferWorkflow.process(transfer);
    }
}
