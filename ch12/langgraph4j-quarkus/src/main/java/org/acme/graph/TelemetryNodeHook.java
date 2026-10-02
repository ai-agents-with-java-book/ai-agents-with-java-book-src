package org.acme.graph;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record TelemetryNodeHook<QAState extends AgentState>(Tracer tracer)
    implements NodeHook.WrapCall<QAState> {


    @Override
    public CompletableFuture<Map<String, Object>> applyWrap(String nodeId, QAState state, RunnableConfig config, AsyncNodeActionWithConfig<QAState> action) {
        final Span span = tracer.spanBuilder("GraphNode: " + nodeId).startSpan();
        try (Scope scope = span.makeCurrent()) {

            span.setAttribute("langgraph.node_id", nodeId)
                .setAttribute("langgraph.state_keys",
                    String.join(",", state.data().keySet()));

            return action.apply(state, config)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        span.recordException(exception);
                    }
                });
        } catch (Exception ignored) {
            // Store potential exceptions.
            span.recordException(ignored);
            throw ignored;
        } finally {
            // Whatever happens above, the span will be closed.
            span.end();
        }
    }
}



