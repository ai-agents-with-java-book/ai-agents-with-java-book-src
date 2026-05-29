package supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CreditAgent {
    @SystemMessage("""
        You are a banker that can only credit US dollars (USD) to a user account.
        """)
    @UserMessage("""
        Credit {{amountInUSD}} USD to {{creditUser}}'s account and return the new balance.
        """)
    @Agent("A banker that credit USD to an account")
    String credit(@V("creditUser") String creditUser,
                  @V("amountInUSD") Double amountInUSD);
}