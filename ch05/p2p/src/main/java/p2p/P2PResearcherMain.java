package p2p;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.p2p.P2PAgent;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.model.chat.ChatModel;

public class P2PResearcherMain {

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) {
        ArxivCrawler arxivCrawler = new ArxivCrawler();

        LiteratureAgent literatureAgent = AgenticServices.agentBuilder(LiteratureAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(arxivCrawler)
                .outputKey("researchFindings")
                .build();
        HypothesisAgent hypothesisAgent = AgenticServices.agentBuilder(HypothesisAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(arxivCrawler)
                .outputKey("hypothesis")
                .build();
        CriticAgent criticAgent = AgenticServices.agentBuilder(CriticAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(arxivCrawler)
                .outputKey("critique")
                .build();
        ValidationAgent validationAgent = AgenticServices.agentBuilder(ValidationAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(arxivCrawler)
                .outputKey("hypothesis")
                .build();
        ScorerAgent scorerAgent = AgenticServices.agentBuilder(ScorerAgent.class)
                .chatModel(CHAT_MODEL)
                .tools(arxivCrawler)
                .outputKey("score")
                .build();

        P2PAgent researcher = AgenticServices.plannerBuilder(P2PAgent.class)
                .subAgents(literatureAgent, hypothesisAgent, criticAgent, validationAgent, scorerAgent)
                .outputKey("hypothesis")
                .planner(() -> new P2PPlanner(CHAT_MODEL, 10, agenticScope -> {
                    if (!agenticScope.hasState("score")) {
                        return false;
                    }
                    double score = agenticScope.readState("score", 0.0);
                    System.out.println("Current hypothesis score: " + score);
                    return score >= 0.85;
                }))
                .build();

        System.out.println(researcher.invoke("produce a research about black holes"));
    }
}
