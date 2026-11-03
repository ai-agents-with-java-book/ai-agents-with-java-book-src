package ch15.router;

import ch15.Models;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.patterns.decisionrouter.DecisionRouterPlanner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.service.V;

public interface DeclarativeSupportRouter {

    @PlannerAgent(outputKey = "answer",
            subAgents = {BillingExpert.class, TechnicalExpert.class,
                         AccountExpert.class})
    String answer(@V("ticket") String ticket);

    @PlannerSupplier
    static Planner planner() {
        return new DecisionRouterPlanner(Models.decisionModel());
    }
}
