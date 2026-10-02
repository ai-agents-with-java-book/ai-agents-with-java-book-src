package org.acme.graph;


import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.acme.ai.Assistant;
import org.acme.ai.AssistantResponse;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.utils.EdgeMappings;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@ApplicationScoped
public class GraphProducer {

    @Inject
    Logger logger;

    @Inject
    Tracer tracer;

    @Produces
    @Singleton
    CompiledGraph<QAState> createHumanGraph(Assistant assistant) throws GraphStateException {
        AsyncNodeAction<QAState> ask = node_async(s -> {
            System.out.println("Sending Question to Model: " + s.question());
            /**AssistantResponse response = assistant.ask(
                s.question()
                    .stream()
                    .collect(Collectors.joining(" "))
            );**/
            AssistantResponse response = new AssistantResponse("Tokyo", false);
            return Map.of("messages", response.response(),
                "human", response.isResponseAQuestion());

        });
        // wait for user response
        AsyncNodeAction<QAState> waitFor = node_async(s -> {
            System.out.println("waiting for human");
            return Map.of();
        });

        // defining nodes and edges
        var graph = new StateGraph<>(QAState.SCHEMA, QAState::new)
            .addBeforeCallNodeHook(new GuardrailNodeHook())
            //.addWrapCallNodeHook(new LoggingNodeHook<>(logger))
            //.addWrapCallNodeHook(new TelemetryNodeHook<>(tracer))
            .addNode("ask", ask)
            .addNode("wait", waitFor)
            .addEdge(GraphDefinition.START, "ask")
            .addEdge("wait", "ask")
            .addConditionalEdges("ask", edge_async(this::routeAnswer),
                EdgeMappings.builder()
                    .to("wait", "human")
                    .to(GraphDefinition.END, "end")
                    .build());



        // 3)   time-travel node state
        var compileConfig = CompileConfig.builder()
            .checkpointSaver(new MemorySaver())
            .interruptAfter("wait")
            .releaseThread(true)
            .build();

        return graph.compile(compileConfig);
    }

    private String routeAnswer(QAState state) {
        if(state.human()) {
            return "human";
        } else {
            return "end";
        }
    }

}
