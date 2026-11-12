package org.acme.support;

import java.util.Map;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public final class SummaryGraph {
    public static CompiledGraph<AgentState> build(SupportSummary summary)
            throws GraphStateException {
        var graph = new StateGraph<AgentState>(AgentState::new);
        graph.addNode("validate", node_async(state -> {
            String request = state.<String>value("request").orElse("");
            if (request.isBlank()) {
                throw new IllegalArgumentException("Support request is required");
            }
            return Map.of("validatedRequest", request);
        }));
        graph.addNode("summarize", node_async(state -> {
            String request = state.<String>value("validatedRequest").orElseThrow();
            return Map.of("summary", summary.summarize(request));
        }));
        graph.addNode("respond", node_async(state -> Map.of(
            "response", state.<String>value("summary").orElseThrow(),
            "status", "completed")));
        graph.addEdge(StateGraph.START, "validate");
        graph.addEdge("validate", "summarize");
        graph.addEdge("summarize", "respond");
        graph.addEdge("respond", StateGraph.END);
        return graph.compile();
    }
}
