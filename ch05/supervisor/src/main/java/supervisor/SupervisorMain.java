package supervisor;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;

public class SupervisorMain {

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();
    private static final ChatModel PLANNER_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) {
        BankTool bankTool = new BankTool();
        bankTool.createAccount("Mario", 1000.0);
        bankTool.createAccount("Georgios", 1000.0);

        WithdrawAgent withdrawAgent = AgenticServices.agentBuilder(WithdrawAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(bankTool)
                .build();

        CreditAgent creditAgent = AgenticServices.agentBuilder(CreditAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(bankTool)
                .build();

        ExchangeAgent exchangeAgent = AgenticServices.agentBuilder(ExchangeAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(new ExchangeTool())
                .build();

        SupervisorAgent bankSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(PLANNER_MODEL)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        bankSupervisor.invoke("Transfer 100 EUR from Mario's account to Georgios' one");
    }
}
