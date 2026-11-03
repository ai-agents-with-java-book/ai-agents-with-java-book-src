package ch15.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.V;

public class MoneyMover {

    @Agent(value = "Executes a money transfer through the banking API",
            outputKey = "receipt")
    public String move(@V("transfer") String transfer, AgenticScope scope) {
        // the key is absent when the gate said no and the approval was skipped
        String approval = scope.readState("approval", "approved");
        if (!approval.equalsIgnoreCase("approved")) {
            return "Transfer held by compliance: " + approval;
        }
        return "Transfer executed: " + transfer;   // the call to the banking API
    }
}
