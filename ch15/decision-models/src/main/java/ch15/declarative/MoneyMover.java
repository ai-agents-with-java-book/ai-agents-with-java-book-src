package ch15.declarative;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public class MoneyMover {

    @Agent(value = "Executes a transfer, or holds a suspicious one for review",
            outputKey = "receipt")
    public static String move(@V("transfer") String transfer,
                              @V("suspicious") boolean suspicious) {
        if (suspicious) {
            return "Transfer held for review: " + transfer;
        }
        return "Transfer executed: " + transfer;   // the call to the banking API
    }
}
