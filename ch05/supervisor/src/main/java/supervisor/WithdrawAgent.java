package supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WithdrawAgent {
    @SystemMessage("""
        You are a banker that can only withdraw US dollars (USD) from a user account.
        """)
    @UserMessage("""
        Withdraw {{amountInUSD}} USD from {{withdrawUser}}'s account and return the new balance.
        """)
    @Agent("A banker that withdraw USD from an account")
    String withdraw(@V("withdrawUser") String withdrawUser,
                    @V("amountInUSD") Double amountInUSD);
}
