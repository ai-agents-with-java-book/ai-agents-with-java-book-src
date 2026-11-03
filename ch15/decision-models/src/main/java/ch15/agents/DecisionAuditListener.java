package ch15.agents;

import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.model.decision.response.DecisionResponse;

public class DecisionAuditListener implements AgentListener {

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        DecisionResponse decision = agentResponse.decisionResponse();
        if (decision != null) {     // null for AI and non-AI agents
            System.out.printf("%s answered %s: %s (%s, %s)%n",
                    agentResponse.agent().name(),
                    agentResponse.output(),
                    decision.answers(),
                    decision.modelName(),
                    decision.tokenUsage());
        }
    }

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }
}
